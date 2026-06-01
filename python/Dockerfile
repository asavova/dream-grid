FROM python:3.12.4-slim-bookworm

WORKDIR /app
COPY python/requirements.txt /app/python/requirements.txt
RUN pip install --no-cache-dir -r /app/python/requirements.txt

COPY python /app/python
WORKDIR /app/python

ENV DREAMGRID_ANALYSIS_HOST=0.0.0.0
ENV DREAMGRID_ANALYSIS_PORT=5005
ENV DREAMGRID_ANALYSIS_BACKEND=rule-based

EXPOSE 5005
CMD ["python", "analysis_api.py"]
