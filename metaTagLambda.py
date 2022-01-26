import boto3
import logging
import json
from html.parser import HTMLParser

logger = logging.getLogger()
logger.setLevel(logging.INFO)
    
s3 = boto3.client('s3')

article_tags = []

class MetaHTMLParser(HTMLParser):
    def handle_starttag(self, tag, attrs):
        global article_tags
        this_article_tags = [ attr[1] for attr in attrs if (tag == "meta") and (('property', 'article:tag') in attrs) and attr[0] == 'content' ]
        if len(this_article_tags) > 0:
            article_tags = article_tags + this_article_tags
            logger.info("Found article tags: %s" % str(article_tags))
        
parser = MetaHTMLParser()
     
def lambda_handler(event, context):
    global article_tags
    logger.info("Received event: %s" % json.dumps(event))
    s3Bucket = event.get("s3Bucket")
    s3ObjectKey = event.get("s3ObjectKey")
    metadata = event.get("metadata")
    
    documentBeforeCDE = s3.get_object(Bucket = s3Bucket, Key = s3ObjectKey)
    beforeCDE = documentBeforeCDE['Body'].read();
    afterCDE = beforeCDE #Do Nothing for now
    new_key = 'cde_output/' + s3ObjectKey
    s3.put_object(Bucket = s3Bucket, Key = new_key, Body=afterCDE)
    article_tags = []
    parser.feed(beforeCDE.decode("utf-8"))
    metaUL = [{
            "name": "ARTICLE_TAGS",
            "value": {
                "stringListValue": article_tags
            }
        }]
    return {
        "version" : "v0",
        "s3ObjectKey": new_key,
        "metadataUpdates": metaUL
    }
