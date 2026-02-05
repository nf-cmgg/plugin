/*
 * Copyright 2026, Center for Medical Genetics Ghent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nfcmgg.plugin.smaple

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic

import java.time.LocalDateTime

import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.OkHttpClient
import okhttp3.MediaType

/**
 * Implements authentication to Smaple
 */
@Slf4j
@CompileStatic
class SmapleAuth {

    final private String authEndpoint
    final private String username
    final private String password
    final private String name
    final private OkHttpClient client = new OkHttpClient()
    final private Integer tokenExpirySeconds = 840 // Token is valid for 15 minutes, refresh after 14 minutes
    final private String authRoute = '/api/v6/auth'

    private String accessToken
    private String refreshToken
    private LocalDateTime accessTokenExpiry

    SmapleAuth(String endpoint, String username, String password) {
        this.authEndpoint = endpoint + authRoute
        this.username = username
        this.password = password
        this.name = UUID.randomUUID().toString()
    }

    void login() {
        String bodyJson = """
        {
            "user": "$username",
            "password": "$password",
            "name": "$name"
        }
        """.stripIndent()
        request(bodyJson, 'login', 'Failed to login to Smaple')
    }

    void refreshIfNecessary() {
        if (LocalDateTime.now().isBefore(accessTokenExpiry)) {
            return
        }
        String bodyJson = """
        {
            "accessToken": "$accessToken",
            "refreshToken": "$refreshToken"
        }
        """.stripIndent()
        request(bodyJson, 'refreshToken', 'Failed to refresh Smaple token')
    }

    private void request(String body, String route, String error) {
        RequestBody requestBody = RequestBody.create(MediaType.parse('application/json; charset=utf-8'), body)
        Request request = new Request.Builder()
            .url("$authEndpoint/$route")
            .post(requestBody)
            .build()

        try (Response response = client.newCall(request).execute()) {
            if (!response.successful) {
                String msg = "${error}: ${response.body().string()}"
                throw new SmapleConnectionException(msg)
            }
            Map respJson = new groovy.json.JsonSlurper().parseText(response.body().string()) as Map
            accessToken = respJson['AccessToken'] as String
            refreshToken = respJson['RefreshToken'] as String
            accessTokenExpiry = LocalDateTime.now().plusSeconds(tokenExpirySeconds)
        }
    }

}
