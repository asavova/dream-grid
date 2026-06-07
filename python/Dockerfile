FROM python:3.12.4-slim-bookworm

# Set working directory for the Python service
WORKDIR /app

# Copy the entire Python analysis service.  Copying the directory before
# installing dependencies allows Docker caching to work effectively when
# requirements change infrequently.
COPY python /app/python

# Install pinned dependencies.  The requirements file is part of the copied
# context so we reference it directly.  --no-cache-dir reduces layer size.
RUN pip install --no-cache-dir -r /app/python/requirements.txt

# Set the working directory to the analysis service root
WORKDIR /app/python

ENV DREAMGRID_ANALYSIS_HOST=0.0.0.0
ENV DREAMGRID_ANALYSIS_PORT=5005
ENV DREAMGRID_ANALYSIS_BACKEND=rule-based

EXPOSE 5005
CMD ["python", "analysis_api.py"]
