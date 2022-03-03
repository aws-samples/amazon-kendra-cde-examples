# Kendra Custom Document Enrichment Examples

This repository contains example AWS Lambda functions that are meant to be used with Amazon Kendra Custom Document Enrichment.

Learn more about how to use Custom Document Enrichment in the blog article
[Enrich your content and metadata to enhance your search experience with custom document enrichment in Amazon Kendra
](https://aws.amazon.com/blogs/machine-learning/enrich-your-content-and-metadata-to-enhance-your-search-experience-with-custom-document-enrichment-in-amazon-kendra/).

## Python Examples

### metaTagLambda.py

This Python script reads the html body and extracts html meta tags, returning them to Amazon Kendra as metadata.

### piiRedactionLambda.py

This Python script uses Amazon Comprehend PII detection to redact content and return the redacted text to Amazon Kendra. It also returns the PII detected entities as metadata for Amazon Kendra.


## Java Examples

### html-metadata-cde

This is a Java project, built with Maven, that is a port from the metaTagLambda.py example.  It reads a file from S3, passed in from Kendra CDE, extracts html meta tags, and returns them to Amazon Kendra. 

Build the project by navigating to the html-metadata-cde folder, then run `mvn package` and copy the `target/html-metadata-cde-0.1.jar` file to AWS Lambda.