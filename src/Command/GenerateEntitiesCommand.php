<?php

namespace App\Command;

use Doctrine\DBAL\Connection;
use Doctrine\DBAL\Schema\Column;
use Doctrine\DBAL\Schema\Table;
use Symfony\Component\Console\Attribute\AsCommand;
use Doctrine\DBAL\Schema\AbstractSchemaManager;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Style\SymfonyStyle;

#[AsCommand(
    name: 'app:generate:entities',
    description: 'Automatically generates entity classes from the database schema',
)]
class GenerateEntitiesCommand extends Command
{
    private Connection $connection;

    // :22 — AbstractSchemaManager is generic over its platform type; use <\Doctrine\DBAL\Platforms\AbstractPlatform>
    /** @var AbstractSchemaManager<\Doctrine\DBAL\Platforms\AbstractPlatform>|null */
    private ?AbstractSchemaManager $schemaManager = null;

    /**
     * Constructor.
     *
     * @param Connection $connection The database connection instance.
     */
    public function __construct(Connection $connection)
    {
        parent::__construct();
        $this->connection = $connection;
    }

    /**
     * Executes the command to generate entity classes.
     *
     * @param InputInterface  $input  Input interface.
     * @param OutputInterface $output Output interface.
     * @return int Command exit status.
     */
    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $io = new SymfonyStyle($input, $output);
        $io->title("Generating Entity Classes from Database...");

        try {
            $schemaManager = $this->getSchemaManager();
            $tables = $schemaManager->listTables();
        } catch (\Exception $e) {
            $io->error("Failed to retrieve database schema: " . $e->getMessage());
            return Command::FAILURE;
        }

        /** @var array<string, list<array{mappedBy: string, targetEntity: string, propertyName: string}>> $oneToManyRelations */
        $oneToManyRelations = [];
        /** @var array<string, list<string>> $manyToOneRelationsName */
        $manyToOneRelationsName = [];
        /** @var array<string, list<string>> $oneToManyRelationsName */
        $oneToManyRelationsName = [];

        // Collect relations and sort tables by their relation count.
        $tableRelationsCount = [];
        foreach ($tables as $table) {
            $foreignKeys = $this->getForeignKeys([$table->getName()]);
            $tableRelationsCount[$table->getName()] = count($foreignKeys);
            $this->collectRelationMetadata($table, $oneToManyRelations, $manyToOneRelationsName, $oneToManyRelationsName);
        }

        usort($tables, function (Table $a, Table $b) use ($tableRelationsCount) {
            return $tableRelationsCount[$a->getName()] <=> $tableRelationsCount[$b->getName()];
        });

        // Generate entities once using the collected relation metadata.
        foreach ($tables as $table) {
            $this->generateEntity($table, $oneToManyRelations, $manyToOneRelationsName, $oneToManyRelationsName);
            $io->success("Generated: src/Entity/" . ucfirst($table->getName()) . ".php");
        }

        $io->success("Entities successfully generated in src/Entity/");
        return Command::SUCCESS;
    }

    /**
     * Retrieves the schema manager instance, caching it to avoid redundant queries.
     *
     * :87 — same generic annotation as the property
     *
     * @return AbstractSchemaManager<\Doctrine\DBAL\Platforms\AbstractPlatform>
     */
    private function getSchemaManager(): AbstractSchemaManager
    {
        if ($this->schemaManager === null) {
            $this->schemaManager = $this->connection->createSchemaManager();
        }
        return $this->schemaManager;
    }

    /**
     * Generates an entity class from a database table.
     *
     * :102 — all three array params are typed with their full shapes
     *
     * @param Table                                                                                  $table                  The database table.
     * @param array<string, list<array{mappedBy: string, targetEntity: string, propertyName: string}>> &$oneToManyRelations   Reference to OneToMany relations.
     * @param array<string, list<string>>                                                            &$manyToOneRelationsName Reference to ManyToOne relation names.
     * @param array<string, list<string>>                                                            &$oneToManyRelationsName Reference to OneToMany relation names.
     */
    private function generateEntity(
        Table $table,
        array &$oneToManyRelations,
        array &$manyToOneRelationsName,
        array &$oneToManyRelationsName
    ): void {
        $className = ucfirst($table->getName());
        $entityCode = "<?php\n\nnamespace App\\Entity;\n\nuse Doctrine\\ORM\\Mapping as ORM;\n\n";

        $imports = $this->generateImports($manyToOneRelationsName, $oneToManyRelationsName, $className);

        $entityCode .= $imports . "\n";
        $entityCode .= "#[ORM\\Entity]\n";
        $entityCode .= "class $className\n{\n";

        // array_values() ensures a list<string> — getColumns() returns array<string> (possibly non-list)
        $primaryKeys = array_values($table->getPrimaryKey()?->getColumns() ?? []);
        $foreignKeys = $this->getForeignKeys([$table->getName()]);

        foreach ($table->getColumns() as $column) {
            $entityCode .= $this->generateProperty($column, $primaryKeys, $foreignKeys, $className);
        }

        if (isset($oneToManyRelations[$className])) {
            $entityCode .= "\n    public function __construct()\n    {\n";
            foreach ($oneToManyRelations[$className] as $relation) {
                $entityCode .= "        \$this->{$relation['propertyName']} = new ArrayCollection();\n";
            }
            $entityCode .= "    }\n";

            foreach ($oneToManyRelations[$className] as $relation) {
                $entityCode .= $this->generateOneToManyProperty($relation);
                $entityCode .= $this->generateRelationMethods($className, $relation['mappedBy'], $relation['targetEntity']);
            }
        }

        foreach ($table->getColumns() as $column) {
            $entityCode .= $this->generateGettersAndSetters($column, $primaryKeys, $foreignKeys);
        }

        $entityCode .= "}\n";

        $filePath = __DIR__ . "/../../src/Entity/$className.php";
        file_put_contents($filePath, $entityCode);
    }

    /**
     * Generates necessary import statements based on detected relations.
     *
     * :160 — typed array params; stale @param $oneToManyRelations removed (parameter.notFound)
     *
     * @param array<string, list<string>> $manyToOneRelationsName ManyToOne relation names keyed by class name.
     * @param array<string, list<string>> $oneToManyRelationsName OneToMany relation names keyed by class name.
     * @param string                      $className              The name of the entity class.
     * @return string Formatted import statements.
     */
    private function generateImports(array $manyToOneRelationsName, array $oneToManyRelationsName, string $className): string
    {
        $imports = [];

        foreach ($manyToOneRelationsName as $key => $values) {
            if ($key === $className) {
                foreach ($values as $value) {
                    $imports[] = "App\\Entity\\$value";
                }
            }
        }

        foreach ($oneToManyRelationsName as $key => $values) {
            if ($key === $className) {
                $imports[] = "Doctrine\\Common\\Collections\\Collection";
                $imports[] = "Doctrine\\Common\\Collections\\ArrayCollection";
                foreach ($values as $value) {
                    $imports[] = "App\\Entity\\$value";
                }
            }
        }

        $imports = array_unique($imports);

        if (count($imports) === 0) {
            return "";
        }

        return "use " . implode(";\nuse ", $imports) . ";\n";
    }

    /**
     * Retrieves foreign key constraints from the database.
     *
     * :198 — typed $tables param and return type
     *
     * @param list<string> $tables List of table names.
     * @return array<string, array{referencedTable: string, referencedColumn: string|null}> Associative array of foreign keys.
     */
    public function getForeignKeys(array $tables): array
    {
        $foreignKeys = [];
        $schemaManager = $this->getSchemaManager();

        foreach ($tables as $tableName) {
            try {
                $foreignKeyObjects = $schemaManager->listTableForeignKeys($tableName);
            } catch (\Throwable $e) {
                continue;
            }

            foreach ($foreignKeyObjects as $foreignKey) {
                $localColumns = $foreignKey->getLocalColumns();
                $foreignColumns = $foreignKey->getForeignColumns();
                $referencedTable = $foreignKey->getForeignTableName();

                foreach ($localColumns as $index => $localColumn) {
                    $foreignKeys[$localColumn] = [
                        'referencedTable' => $referencedTable,
                        'referencedColumn' => $foreignColumns[$index] ?? null,
                    ];
                }
            }
        }

        return $foreignKeys;
    }

    /**
     * Collects relation metadata for all tables.
     *
     * :227 — all three array params are typed with their full shapes
     *
     * @param Table                                                                                     $table                  The database table.
     * @param array<string, list<array{mappedBy: string, targetEntity: string, propertyName: string}>> &$oneToManyRelations     Reference to OneToMany relations.
     * @param array<string, list<string>>                                                               &$manyToOneRelationsName Reference to ManyToOne relation names.
     * @param array<string, list<string>>                                                               &$oneToManyRelationsName Reference to OneToMany relation names.
     */
    private function collectRelationMetadata(
        Table $table,
        array &$oneToManyRelations,
        array &$manyToOneRelationsName,
        array &$oneToManyRelationsName
    ): void {
        $className = ucfirst($table->getName());
        $foreignKeys = $this->getForeignKeys([$table->getName()]);

        foreach ($foreignKeys as $columnName => $foreignKeyData) {
            $relatedClassName = ucfirst($foreignKeyData['referencedTable']);
            $propertyName = $this->getRelationPropertyName($columnName, $relatedClassName);

            $manyToOneRelationsName[$className][] = $relatedClassName;
            $oneToManyRelationsName[$relatedClassName][] = $className;
            $oneToManyRelations[$relatedClassName][] = [
                'mappedBy' => $propertyName,
                'targetEntity' => $className,
                'propertyName' => lcfirst($className) . 's',
            ];
        }
    }

    private function getRelationPropertyName(string $columnName, string $relatedClassName): string
    {
        if (str_ends_with($columnName, '_id')) {
            return lcfirst(substr($columnName, 0, -3));
        }

        return lcfirst($relatedClassName);
    }

    /**
     * Generates relation methods for OneToMany and ManyToOne relations.
     *
     * @param string $currentEntity The current entity name.
     * @param string $propertyName  The property name representing the relation.
     * @param string $relatedEntity The related entity name.
     * @return string The generated method code.
     */
    private function generateRelationMethods(string $currentEntity, string $propertyName, string $relatedEntity): string
    {
        $collectionType = "Collection";
        $relatedEntityClass = ucfirst($relatedEntity);
        $currentEntityClass = ucfirst($currentEntity);
        $relatedEntityVariable = lcfirst($relatedEntity);

        return "
        public function get" . $relatedEntityClass . "s(): $collectionType
        {
            return \$this->" . $relatedEntityVariable . "s;
        }
    
        public function add{$relatedEntityClass}({$relatedEntityClass} \${$relatedEntityVariable}): self
        {
            if (!\$this->{$relatedEntityVariable}s->contains(\${$relatedEntityVariable})) {
                \$this->{$relatedEntityVariable}s[] = \${$relatedEntityVariable};
                \${$relatedEntityVariable}->set" . ucfirst($propertyName) . "(\$this);
            }
    
            return \$this;
        }
    
        public function remove{$relatedEntityClass}({$relatedEntityClass} \${$relatedEntityVariable}): self
        {
            if (\$this->{$relatedEntityVariable}s->removeElement(\${$relatedEntityVariable})) {
                if (\${$relatedEntityVariable}->get" . ucfirst($propertyName) . "() === \$this) {
                    \${$relatedEntityVariable}->set" . ucfirst($propertyName) . "(null);
                }
            }
    
            return \$this;
        }\n";
    }

    /**
     * @param array{mappedBy: string, targetEntity: string, propertyName: string} $relation
     */
    private function generateOneToManyProperty(array $relation): string
    {
        return "
    #[ORM\\OneToMany(mappedBy: \"{$relation['mappedBy']}\", targetEntity: {$relation['targetEntity']}::class)]\n    private Collection $" . $relation['propertyName'] . ";\n";
    }

    /**
     * Generates entity properties based on database columns.
     *
     * :316 — typed $primaryKeys and $foreignKeys; stale @param tags for removed params dropped
     *
     * @param Column                                                                          $column      The database column.
     * @param list<string>                                                                    $primaryKeys List of primary key column names.
     * @param array<string, array{referencedTable: string, referencedColumn: string|null}>   $foreignKeys Foreign key metadata.
     * @param string                                                                          $className   The entity class name.
     * @return string The generated property code.
     */
    private function generateProperty(Column $column, array $primaryKeys, array $foreignKeys, string $className): string
    {
        $columnName = $column->getName();
        $typeClass = get_class($column->getType());
        $length = $column->getLength();
        $isPrimaryKey = in_array($columnName, $primaryKeys);
        $isForeignKey = isset($foreignKeys[$columnName]);
        $isNullable = !$column->getNotnull();

        $doctrineType = match ($typeClass) {
            'Doctrine\DBAL\Types\IntegerType' => 'integer',
            'Doctrine\DBAL\Types\BigIntType' => 'bigint',
            'Doctrine\DBAL\Types\SmallIntType' => 'smallint',
            'Doctrine\DBAL\Types\BooleanType' => 'boolean',
            'Doctrine\DBAL\Types\DateTimeType', 'Doctrine\DBAL\Types\TimestampType' => 'datetime',
            'Doctrine\DBAL\Types\DateType' => 'date',
            'Doctrine\DBAL\Types\TextType' => 'text',
            'Doctrine\DBAL\Types\DecimalType', 'Doctrine\DBAL\Types\FloatType', 'Doctrine\DBAL\Types\DoubleType' => 'float',
            'Doctrine\DBAL\Types\StringType', 'Doctrine\DBAL\Types\VarCharType' => 'string',
            default => 'string',
        };

        $lengthAnnotation = ($doctrineType === 'string' && $length) ? ", length: $length" : "";

        $propertyCode = "\n    " . ($isPrimaryKey ? "#[ORM\\Id]\n    " : "");

        if ($isForeignKey) {
            $relatedEntity = $foreignKeys[$columnName]['referencedTable'];
            $relatedClassName = ucfirst($relatedEntity);
            $propertyName = $this->getRelationPropertyName($columnName, $relatedClassName);
            $nullable = $isNullable ? 'true' : 'false';
            $referencedColumn = $foreignKeys[$columnName]['referencedColumn'] ?? 'id';

            $propertyCode .= "    #[ORM\\ManyToOne(targetEntity: $relatedClassName::class, inversedBy: \"" . lcfirst($className) . "s\")]\n";
            $propertyCode .= "    #[ORM\\JoinColumn(name: '$columnName', referencedColumnName: '$referencedColumn', nullable: $nullable, onDelete: 'CASCADE')]\n";
            $propertyCode .= "    private ?$relatedClassName \$$propertyName = null;\n";

            return $propertyCode;
        } else {
            $propertyCode .= "#[ORM\\Column(type: \"$doctrineType\"$lengthAnnotation)]\n";
            $propertyCode .= "    private " . $this->getPHPTypeFromDoctrine($doctrineType) . " \$$columnName;\n";
        }

        return $propertyCode;
    }

    private function getPHPTypeFromDoctrine(string $doctrineType): string
    {
        $mapping = [
            'integer' => 'int',
            'smallint' => 'int',
            'bigint' => 'string',
            'string' => 'string',
            'text' => 'string',
            'boolean' => 'bool',
            'decimal' => 'string',
            'float' => 'float',
            'date' => '\DateTimeInterface',
            'datetime' => '\DateTimeInterface',
            'datetimetz' => '\DateTimeInterface',
            'time' => '\DateTimeInterface',
            'array' => 'array',
            'json' => 'array',
            'object' => 'object',
            'binary' => 'string',
            'blob' => 'string',
            'guid' => 'string',
        ];

        return $mapping[$doctrineType] ?? 'mixed';
    }

    /**
     * Generates getter and setter methods for entity properties.
     *
     * :411 — typed $primaryKeys and $foreignKeys
     *
     * @param Column                                                                        $column      The database column.
     * @param list<string>                                                                  $primaryKeys List of primary key column names.
     * @param array<string, array{referencedTable: string, referencedColumn: string|null}> $foreignKeys Foreign key metadata.
     * @return string The generated getter and setter methods.
     */
    private function generateGettersAndSetters(Column $column, array $primaryKeys, array $foreignKeys): string
    {
        $columnName = $column->getName();
        $isForeignKey = isset($foreignKeys[$columnName]);
        $isPrimaryKey = in_array($columnName, $primaryKeys);

        if ($isForeignKey) {
            $relatedClassName = ucfirst($foreignKeys[$columnName]['referencedTable']);
            $propertyName = $this->getRelationPropertyName($columnName, $relatedClassName);
            $methodName = ucfirst($propertyName);

            return "
    public function get$methodName(): ?$relatedClassName
    {
        return \$this->$propertyName;
    }

    public function set$methodName(?$relatedClassName \$$propertyName): self
    {
        \$this->$propertyName = \$$propertyName;
        return \$this;
    }\n";
        }

        $propertyName = $columnName;
        $methodName = ucfirst($propertyName);
        $doctrineType = match (get_class($column->getType())) {
            'Doctrine\\DBAL\\Types\\IntegerType' => 'integer',
            'Doctrine\\DBAL\\Types\\BigIntType' => 'bigint',
            'Doctrine\\DBAL\\Types\\SmallIntType' => 'smallint',
            'Doctrine\\DBAL\\Types\\BooleanType' => 'boolean',
            'Doctrine\\DBAL\\Types\\DateTimeType', 'Doctrine\\DBAL\\Types\\TimestampType' => 'datetime',
            'Doctrine\\DBAL\\Types\\DateType' => 'date',
            'Doctrine\\DBAL\\Types\\TextType' => 'text',
            'Doctrine\\DBAL\\Types\\DecimalType', 'Doctrine\\DBAL\\Types\\FloatType', 'Doctrine\\DBAL\\Types\\DoubleType' => 'float',
            'Doctrine\\DBAL\\Types\\StringType', 'Doctrine\\DBAL\\Types\\VarCharType' => 'string',
            default => 'string',
        };
        $phpType = $this->getPHPTypeFromDoctrine($doctrineType);
        $isNullable = !$column->getNotnull();
        $returnType = $phpType !== 'mixed' ? ($isNullable ? '?'.$phpType : $phpType) : '';
        $paramType = $returnType;

        if ($isPrimaryKey) {
            return "
    public function get$methodName()" . ($returnType ? ": $returnType" : "") . "
    {
        return \$this->$propertyName;
    }\n";
        }

        return "
    public function get$methodName()" . ($returnType ? ": $returnType" : "") . "
    {
        return \$this->$propertyName;
    }

    public function set$methodName(" . ($paramType ? "$paramType " : "") . "\$value): self
    {
        \$this->$propertyName = \$value;
        return \$this;
    }\n";
    }
}