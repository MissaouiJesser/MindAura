package tn.esprit.entities;

public class QuestionReponse {

    // ============================
    // Champs (correspondent à la table question_reponse)
    // ============================
    private int idQuestionReponse;   // id_question_reponse - PK AUTO_INCREMENT
    private String texteQuestion;    // texte_question       - longtext, NOT NULL
    private String typeQuestion;     // type_question        - varchar(255), NOT NULL
    private int ordreQuestion;       // ordre_question       - int(11), NOT NULL
    private String option1;          // option1              - varchar(255), NULL
    private int score1;              // score1               - int(11), NULL
    private String option2;          // option2              - varchar(255), NULL
    private int score2;              // score2               - int(11), NULL
    private String option3;          // option3              - varchar(255), NULL
    private int score3;              // score3               - int(11), NULL
    private String option4;          // option4              - varchar(255), NULL
    private int score4;              // score4               - int(11), NULL
    private String option5;          // option5              - varchar(255), NULL
    private int score5;              // score5               - int(11), NULL
    private boolean estObligatoire;  // est_obligatoire      - tinyint(4), NULL
    private int idTestId;            // id_test_id           - FK, int(11), NOT NULL

    // ============================
    // Constructeur complet (avec ID) - pour afficher / modifier les données
    // ============================
    public QuestionReponse(int idQuestionReponse, int idTestId,
                           String texteQuestion, String typeQuestion, int ordreQuestion,
                           String option1, int score1,
                           String option2, int score2,
                           String option3, int score3,
                           String option4, int score4,
                           String option5, int score5,
                           boolean estObligatoire) {
        this.idQuestionReponse = idQuestionReponse;
        this.idTestId          = idTestId;
        this.texteQuestion     = texteQuestion;
        this.typeQuestion      = typeQuestion;
        this.ordreQuestion     = ordreQuestion;
        this.option1           = option1;
        this.score1            = score1;
        this.option2           = option2;
        this.score2            = score2;
        this.option3           = option3;
        this.score3            = score3;
        this.option4           = option4;
        this.score4            = score4;
        this.option5           = option5;
        this.score5            = score5;
        this.estObligatoire    = estObligatoire;
    }

    // ============================
    // Constructeur sans ID - pour l'insertion (INSERT)
    // ============================
    public QuestionReponse(int idTestId,
                           String texteQuestion, String typeQuestion, int ordreQuestion,
                           String option1, int score1,
                           String option2, int score2,
                           String option3, int score3,
                           String option4, int score4,
                           String option5, int score5,
                           boolean estObligatoire) {
        this.idTestId       = idTestId;
        this.texteQuestion  = texteQuestion;
        this.typeQuestion   = typeQuestion;
        this.ordreQuestion  = ordreQuestion;
        this.option1        = option1;
        this.score1         = score1;
        this.option2        = option2;
        this.score2         = score2;
        this.option3        = option3;
        this.score3         = score3;
        this.option4        = option4;
        this.score4         = score4;
        this.option5        = option5;
        this.score5         = score5;
        this.estObligatoire = estObligatoire;
    }

    // ============================
    // Constructeur simplifié - pour questions sans options (texte_libre)
    // ============================
    public QuestionReponse(int idTestId, String texteQuestion,
                           String typeQuestion, int ordreQuestion) {
        this.idTestId       = idTestId;
        this.texteQuestion  = texteQuestion;
        this.typeQuestion   = typeQuestion;
        this.ordreQuestion  = ordreQuestion;
        this.estObligatoire = true;
    }

    // ============================
    // Constructeur vide
    // ============================
    public QuestionReponse() {
        this.estObligatoire = true;
    }

    // ============================
    // Getters & Setters
    // ============================
    public int getIdQuestionReponse() { return idQuestionReponse; }
    public void setIdQuestionReponse(int idQuestionReponse) { this.idQuestionReponse = idQuestionReponse; }

    public int getIdTestId() { return idTestId; }
    public void setIdTestId(int idTestId) { this.idTestId = idTestId; }

    public String getTexteQuestion() { return texteQuestion; }
    public void setTexteQuestion(String texteQuestion) { this.texteQuestion = texteQuestion; }

    public String getTypeQuestion() { return typeQuestion; }
    public void setTypeQuestion(String typeQuestion) { this.typeQuestion = typeQuestion; }

    public int getOrdreQuestion() { return ordreQuestion; }
    public void setOrdreQuestion(int ordreQuestion) { this.ordreQuestion = ordreQuestion; }

    public String getOption1() { return option1; }
    public void setOption1(String option1) { this.option1 = option1; }

    public int getScore1() { return score1; }
    public void setScore1(int score1) { this.score1 = score1; }

    public String getOption2() { return option2; }
    public void setOption2(String option2) { this.option2 = option2; }

    public int getScore2() { return score2; }
    public void setScore2(int score2) { this.score2 = score2; }

    public String getOption3() { return option3; }
    public void setOption3(String option3) { this.option3 = option3; }

    public int getScore3() { return score3; }
    public void setScore3(int score3) { this.score3 = score3; }

    public String getOption4() { return option4; }
    public void setOption4(String option4) { this.option4 = option4; }

    public int getScore4() { return score4; }
    public void setScore4(int score4) { this.score4 = score4; }

    public String getOption5() { return option5; }
    public void setOption5(String option5) { this.option5 = option5; }

    public int getScore5() { return score5; }
    public void setScore5(int score5) { this.score5 = score5; }

    public boolean isEstObligatoire() { return estObligatoire; }
    public void setEstObligatoire(boolean estObligatoire) { this.estObligatoire = estObligatoire; }

    // ============================
    // toString
    // ============================
    @Override
    public String toString() {
        return "QuestionReponse{" +
                "idQuestionReponse=" + idQuestionReponse +
                ", idTestId=" + idTestId +
                ", texteQuestion='" + texteQuestion + '\'' +
                ", typeQuestion='" + typeQuestion + '\'' +
                ", ordreQuestion=" + ordreQuestion +
                ", option1='" + option1 + '\'' +
                ", score1=" + score1 +
                ", option2='" + option2 + '\'' +
                ", score2=" + score2 +
                ", option3='" + option3 + '\'' +
                ", score3=" + score3 +
                ", option4='" + option4 + '\'' +
                ", score4=" + score4 +
                ", option5='" + option5 + '\'' +
                ", score5=" + score5 +
                ", estObligatoire=" + estObligatoire +
                '}';
    }
}