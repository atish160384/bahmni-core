package org.bahmni.module.referncedatafeedclient.domain;

import org.bahmni.module.referncedatafeedclient.ReferenceDataFeedProperties;
import org.bahmni.webclients.ConnectionDetails;
import org.bahmni.webclients.HttpClient;

import java.io.IOException;

public class WebClientFactory {

    public static HttpClient createReferenceDataClient(ReferenceDataFeedProperties referenceDataFeedProperties) throws IOException {
        return new HttpClient(new ConnectionDetails(referenceDataFeedProperties.getReferenceDataUri(), null, null,
                referenceDataFeedProperties.getConnectTimeout(), referenceDataFeedProperties.getReadTimeout()));
    }
}