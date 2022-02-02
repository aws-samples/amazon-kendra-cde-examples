import boto3
import logging
import json

logger = logging.getLogger()
logger.setLevel(logging.INFO)
    
s3 = boto3.client('s3')

comprehend = boto3.client(service_name='comprehend')
MAX_REQ_SIZE=4000
CUT_OFF_SCORE=0.9

def document_handler(doc_text):
    output = ""
    cursor = 0
    pii_entities = []
    for i in range(0, len(doc_text), MAX_REQ_SIZE):
        response = comprehend.detect_pii_entities(Text=doc_text[i:i+MAX_REQ_SIZE], LanguageCode='en')
        for e in response["Entities"]:
            if (e["Score"] > CUT_OFF_SCORE):
                output += (doc_text[cursor:i+e["BeginOffset"]] + ''.join(['*' for d in range(e["BeginOffset"],e["EndOffset"])]))
                cursor = i+e["EndOffset"]
            if not (e["Type"] in pii_entities):
                pii_entities.append(e["Type"])
    output += doc_text[cursor:]
    return (output, pii_entities)
     
def lambda_handler(event, context):
    logger.info("Received event: %s" % json.dumps(event))
    s3Bucket = event.get("s3Bucket")
    s3ObjectKey = event.get("s3ObjectKey")
    
    documentBeforeCDE = s3.get_object(Bucket = s3Bucket, Key = s3ObjectKey)
    beforeCDE = documentBeforeCDE['Body'].read()
    kendra_document = json.loads(beforeCDE)
    (afterCDE, pii_entities) = document_handler(kendra_document.get("textContent").get("documentBodyText"))
    documentAfterCDE = {
        "textContent" : {
            "documentBodyText": afterCDE
        }
    }
    new_key = 'cde_output/' + s3ObjectKey + '.json'
    s3.put_object(Bucket = s3Bucket, Key = new_key, Body=json.dumps(documentAfterCDE))
    metaUL = [{
            "name": "PII_ENTITIES",
            "value": {
                "stringListValue": pii_entities
            }
        }]
    return {
        "version" : "v0",
        "s3ObjectKey": new_key,
        "metadataUpdates": metaUL
    }