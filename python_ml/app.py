from flask import Flask, request, jsonify
from transformers import pipeline
import json
import sys
import io

# ----------------------------------------------------
# 1. 서버 및 모델 초기화
# ----------------------------------------------------
app = Flask(__name__)

# 표준 출력을 UTF-8로 설정 (오류 메시지 깨짐 방지)
# sys.stdout = io.TextIOWrapper(sys.stdout.detach(), encoding='utf-8')
# sys.stderr = io.TextIOWrapper(sys.stderr.detach(), encoding='utf-8') # ⚠️ 에러 출력 스트림도 UTF-8로 설정

print("[Python ML Server] 모델 로딩 중... (최초 1회만 실행)")

# 모델: snunlp/kr-finbert-sc (한국어 감정 분석 모델)
sentiment_analyzer = pipeline(
    "sentiment-analysis",
    model="snunlp/kr-finbert-sc",
    tokenizer="snunlp/kr-finbert-sc"
)

print("[Python ML Server] 모델 로딩 완료.")

# 모델 출력 레이블에 맞춰 긍정/부정/중립 매핑
score_mapping = {
    "positive": "긍정",
    "negative": "부정",
    "neutral": "중립"
}

# 기사 본문 감정 분석 함수
def analyze_summary(summary):
    if not summary:
        return "중립"

    # analysis 결과는 [{ 'label': 'positive', 'score': 0.99... }] 형태
    analysis = sentiment_analyzer(summary[:512])[0]
    return score_mapping.get(analysis["label"].lower(), "중립")


# ----------------------------------------------------
# 2. 분석 요청 처리 API 엔드포인트
# ----------------------------------------------------
@app.route('/analyze', methods=['POST'])
def analyze_news_list():
    try:
        # Spring Boot에서 전달된 뉴스 리스트 (JSON 배열)를 받습니다.
        news_data = request.get_json()

        if not news_data or not isinstance(news_data, list):
            # 400 Bad Request
            return jsonify({"error": "뉴스 데이터 형식이 올바르지 않습니다. 리스트 형태여야 합니다."}), 400
        
        results = []
        for i, article in enumerate(news_data):
            # ⚠️ 모든 필수 필드의 존재 여부를 확인합니다.
            article_id = article.get("id")
            title = article.get("title")
            summary = article.get("summary")

            if article_id is None or title is None or summary is None:
                # 데이터 구조 오류를 명확히 출력하고 400 Bad Request 반환
                required_fields = ["id", "title", "summary"]
                missing_fields = [f for f in required_fields if article.get(f) is None]
                
                print(f"데이터 구조 오류 (인덱스 {i}): 필수 필드 누락 -> {', '.join(missing_fields)}", file=sys.stderr)
                
                return jsonify({
                    "error": "전달된 뉴스 기사 데이터에 필수 필드가 누락되었습니다.",
                    "missing_fields": missing_fields,
                    "article_index": i
                }), 400 # 400 Bad Request 반환

            sentiment_label = analyze_summary(summary)

            results.append({
                "id": article_id,
                "title": title,
                "sentiment": sentiment_label
            })

        print(f"분석 완료: {len(results)}개의 기사 분석 결과 반환")
        return jsonify(results)

    except Exception as e:
        import traceback # 상세한 오류 추적을 위한 임포트
        error_trace = traceback.format_exc()
        
        # 500 오류 발생 시, 파이썬 터미널에 상세 Traceback 출력
        print(f"분석 API 처리 중 치명적인 오류 발생:\n{error_trace}", file=sys.stderr)
        
        # Spring Boot로 상세하지 않은 500 응답 반환
        return jsonify({"error": f"분석 서버 내부 처리 오류. 상세 내용은 서버 로그를 확인하세요."}), 500

if __name__ == '__main__':
    # 8000번 포트로 서버를 실행합니다.
    app.run(host='0.0.0.0', port=8000)