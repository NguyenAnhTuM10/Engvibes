FROM python:3.12-slim

# ffmpeg để convert webm/mp3 → wav cho librosa
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg libsndfile1 espeak-ng && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .

# CPU-only torch trước (tránh download CUDA, tiết kiệm ~2GB)
RUN pip install --no-cache-dir \
    torch torchaudio --index-url https://download.pytorch.org/whl/cpu

RUN pip install --no-cache-dir -r requirements.txt

# NLTK data cho g2p_en
RUN python -c "import nltk; nltk.download('averaged_perceptron_tagger_eng'); nltk.download('cmudict')"

# Pre-download wav2vec2 phoneme model lúc build → cold start không cần tải
# facebook/wav2vec2-lv-60-espeak-cv-ft: fine-tuned trên CommonVoice 60-lang,
# output là eSpeak IPA phonemes — phù hợp nhất cho phoneme-level pronunciation assessment.
RUN python -c "\
from transformers import Wav2Vec2Processor, Wav2Vec2ForCTC; \
Wav2Vec2Processor.from_pretrained('facebook/wav2vec2-lv-60-espeak-cv-ft'); \
Wav2Vec2ForCTC.from_pretrained('facebook/wav2vec2-lv-60-espeak-cv-ft'); \
print('Model pre-downloaded OK')"

COPY main.py .

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
