// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

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

public class App implements RequestHandler<Map<String,Object>, Map<String, Object>>
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
    public Map<String, Object> handleRequest(Map<String,Object> event, Context context)
    {
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(System.getenv("AWS_REGION")).build();
        
        LambdaLogger logger = context.getLogger();
        String s3Bucket = (String)event.get("s3Bucket");
        String s3ObjectKey = (String)event.get("s3ObjectKey");
        Map<String, Object> dictionary = new HashMap<>();

        try {

            logger.log("Starting download from s3...");
            S3Object o = s3.getObject(s3Bucket, s3ObjectKey);
            S3ObjectInputStream s3is = o.getObjectContent();

            String beforeCDE = new BufferedReader(new InputStreamReader(s3is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
            s3is.close();

            logger.log("Finished download. Starting Metatag scan...");

            List<String> article_tags = getMetadataTags(beforeCDE); 
            logger.log("Finished metatag scan. Writing output file...");

            String afterCDE = beforeCDE.toString(); 
            //you can manipulate afterCDE here.
            String new_key = "cde_output/" + s3ObjectKey;
            s3.putObject(s3Bucket, new_key, afterCDE);
            logger.log("Finished writing output file.");
            Map<String, Object> stringListValue = new HashMap<>();
            stringListValue.put("stringListValue", article_tags);
            Map<String, Object> metaUL = new HashMap<>();
            metaUL.put("name", "ARTICLE_TAGS");
            metaUL.put("value", stringListValue);
            List<Map<String, Object>> metaUL_List = new ArrayList<>();
            metaUL_List.add(metaUL);
            dictionary.put("version","v0");
            dictionary.put("s3ObjectKey",new_key); 
            dictionary.put("metadataUpdates",metaUL_List);
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

        return dictionary;
    }
}
