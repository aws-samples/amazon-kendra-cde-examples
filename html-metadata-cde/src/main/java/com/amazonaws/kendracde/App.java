package com.amazonaws.kendracde;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements; 

public class App implements RequestHandler<Map<String,String>, String>
{
    private List<String> getMetadataTags(String html)
    {
        List<String> article_tags = new ArrayList<String>();
        Document doc = Jsoup.parse(html);
        Elements metaTags = doc.getElementsByTag("meta");
        
        for (Element metaTag : metaTags) {
            if("article:tag".equals(metaTag.attr("property"))) {
                article_tags.add(metaTag.attr("content"));
            }
        }

        return article_tags;
    }

    @Override
    public String handleRequest(Map<String,String> event, Context context)
    {
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.DEFAULT_REGION).build();
        
        LambdaLogger logger = context.getLogger();
        logger.log("Received event: " + event.toString());

        String s3Bucket = event.get("s3Bucket");
        String s3ObjectKey = event.get("s3ObjectKey");
        //String metadata = event.get("metadata"); //this is here because its in the python example
        Map<String, Object> dictionary = new HashMap<>();

        try {
            S3Object o = s3.getObject(s3Bucket, s3ObjectKey);
            S3ObjectInputStream s3is = o.getObjectContent();

            String beforeCDE = new BufferedReader(new InputStreamReader(s3is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
            s3is.close();

            List<String> article_tags = getMetadataTags(beforeCDE); 
            String afterCDE = beforeCDE.toString(); 
            //you can manipulate afterCDE here.
            String new_key = "cde_output/" + s3ObjectKey;
            s3.putObject(s3Bucket, new_key, afterCDE);

            Map<String, Object> stringListValue = new HashMap<>();
            stringListValue.put("stringListValue", article_tags);
            Map<String, Object> metaUL = new HashMap<>();
            metaUL.put("name", "ARTICLE_TAGS");
            metaUL.put("value", stringListValue);
            dictionary.put("version","v0");
            dictionary.put("s3ObjectKey","v0"); 
            dictionary.put("metadataUpdates",metaUL);
        } catch (AmazonServiceException e) {
            logger.log(e.getErrorMessage());
            System.exit(1);
        } catch (FileNotFoundException e) {
            logger.log(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            logger.log(e.getMessage());
            System.exit(1);
        }

        String response = new JSONObject(dictionary).toString();
        return response;
    }
}
