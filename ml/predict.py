from flask import Blueprint, request, jsonify
import joblib
import numpy as np
import requests
from datetime import datetime

predict_bp = Blueprint('predict', __name__)

# Chargement des modèles
model      = joblib.load('models/xgboost_weather.pkl')
le_season  = joblib.load('models/label_encoder_season.pkl')
le_weather = joblib.load('models/label_encoder_weather.pkl')

def get_season(month):
    if month in [12, 1, 2]: return 'winter'
    if month in [3, 4, 5]:  return 'spring'
    if month in [6, 7, 8]:  return 'summer'
    return 'autumn'

def predict_for_day(temp_max, temp_min, precipitation, windspeed, day, month):
    """Fonction commune de prédiction"""
    season         = get_season(month)
    season_encoded = le_season.transform([season])[0]

    features = np.array([[
        temp_max, temp_min, precipitation,
        windspeed, day, month, season_encoded
    ]])

    prediction    = model.predict(features)[0]
    probabilities = model.predict_proba(features)[0]
    label         = le_weather.inverse_transform([prediction])[0]

    return {
        'prediction'   : label,
        'confidence'   : round(float(max(probabilities)) * 100, 2),
        'probabilities': {
            l: round(float(p) * 100, 2)
            for l, p in zip(le_weather.classes_, probabilities)
        }
    }

def fetch_real_weather(days=7):
    """Récupère les 7 prochains jours depuis Open-Meteo"""
    url = "https://api.open-meteo.com/v1/forecast"
    params = {
        "latitude"      : 36.8,
        "longitude"     : 10.18,
        "daily"         : [
            "temperature_2m_max",
            "temperature_2m_min",
            "precipitation_sum",
            "windspeed_10m_max"
        ],
        "forecast_days" : days,
        "timezone"      : "Africa/Tunis"
    }

    response = requests.get(url, params=params)
    data     = response.json()
    daily    = data["daily"]

    result = []
    for i in range(days):
        date = datetime.strptime(daily["time"][i], "%Y-%m-%d")
        result.append({
            "date"         : daily["time"][i],
            "temp_max"     : daily["temperature_2m_max"][i],
            "temp_min"     : daily["temperature_2m_min"][i],
            "precipitation": daily["precipitation_sum"][i] or 0,
            "windspeed"    : daily["windspeed_10m_max"][i] or 0,
            "day"          : date.day,
            "month"        : date.month,
        })

    return result

@predict_bp.route('/api/predict/weather/forecast', methods=['GET'])
def forecast():
    try:
        days    = fetch_real_weather(days=7)
        results = []

        jours_fr = {
            "Monday"   : "Lundi",
            "Tuesday"  : "Mardi",
            "Wednesday": "Mercredi",
            "Thursday" : "Jeudi",
            "Friday"   : "Vendredi",
            "Saturday" : "Samedi",
            "Sunday"   : "Dimanche"
        }

        for i, day in enumerate(days):
            prediction = predict_for_day(
                day["temp_max"],
                day["temp_min"],
                day["precipitation"],
                day["windspeed"],
                day["day"],
                day["month"]
            )

            # Label du jour
            if i == 0:   label = "Aujourd'hui"
            elif i == 1: label = "Demain"
            else:
                date_obj = datetime.strptime(day["date"], "%Y-%m-%d")
                label = jours_fr.get(date_obj.strftime("%A"), date_obj.strftime("%A"))

            results.append({
                "date"         : day["date"],
                "label"        : label,
                "temp_max"     : day["temp_max"],
                "temp_min"     : day["temp_min"],
                "precipitation": day["precipitation"],
                "windspeed"    : day["windspeed"],
                **prediction
            })

        return jsonify({"success": True, "forecast": results})

    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

# Ancien endpoint — garde-le pour compatibilité
@predict_bp.route('/api/predict/weather', methods=['POST'])
def predict_weather():
    try:
        data          = request.get_json()
        temp_max      = float(data['temp_max'])
        temp_min      = float(data['temp_min'])
        precipitation = float(data.get('precipitation', 0))
        windspeed     = float(data.get('windspeed', 0))
        day           = int(data['day'])
        month         = int(data['month'])

        result = predict_for_day(
            temp_max, temp_min, precipitation, windspeed, day, month
        )

        return jsonify({"success": True, **result})

    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500