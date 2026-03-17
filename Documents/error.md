supabse eventlog error
{
  "event_message": "POST | 400 | https://vazjljkzryzrsxragumb.supabase.co/rest/v1/event_logs | okhttp/4.12.0",
  "id": "dc843376-bb92-49a1-8249-4c367d9da18e",
  "metadata": [
    {
      "load_balancer_experimental_routing": null,
      "load_balancer_geo_aware_info": [],
      "load_balancer_redirect_identifier": null,
      "logflare_worker": [
        {
          "worker_id": "IDM23R"
        }
      ],
      "request": [
        {
          "cf": [
            {
              "asOrganization": "Excitel Broadband Private Limited",
              "asn": null,
              "botManagement": [
                {
                  "corporateProxy": null,
                  "detectionIds": [],
                  "ja3Hash": "f79b6bad2ad0641e1921aef10262856b",
                  "ja4": "t13d1513h2_8daaf6152771_eca864cca44a",
                  "ja4Signals": [],
                  "jsDetection": [],
                  "score": null,
                  "staticResource": null,
                  "verifiedBot": null
                }
              ],
              "city": "Bengaluru",
              "clientAcceptEncoding": null,
              "clientTcpRtt": null,
              "clientTrustScore": 98,
              "colo": "BLR",
              "continent": null,
              "country": "IN",
              "edgeRequestKeepAliveStatus": null,
              "httpProtocol": "HTTP/2",
              "isEUCountry": null,
              "latitude": null,
              "longitude": null,
              "metroCode": null,
              "postalCode": "562114",
              "region": "Karnataka",
              "regionCode": null,
              "requestPriority": null,
              "timezone": "Asia/Kolkata",
              "tlsCipher": null,
              "tlsClientAuth": [],
              "tlsClientCiphersSha1": null,
              "tlsClientExtensionsSha1": null,
              "tlsClientExtensionsSha1Le": null,
              "tlsClientHelloLength": null,
              "tlsClientRandom": null,
              "tlsExportedAuthenticator": [],
              "tlsVersion": null,
              "verifiedBotCategory": null
            }
          ],
          "headers": [
            {
              "accept": "application/json",
              "cf_cache_status": null,
              "cf_connecting_ip": "103.215.237.105",
              "cf_ipcountry": "IN",
              "cf_ray": "9ddc9313ff6ba403",
              "content_length": "201",
              "content_location": null,
              "content_range": null,
              "content_type": "application/json; charset=UTF-8",
              "date": null,
              "host": "vazjljkzryzrsxragumb.supabase.co",
              "prefer": "return=representation",
              "range": null,
              "referer": null,
              "sb_gateway_mode": null,
              "sb_gateway_version": null,
              "user_agent": "okhttp/4.12.0",
              "x_client_info": null,
              "x_forwarded_for": null,
              "x_forwarded_host": null,
              "x_forwarded_proto": "https",
              "x_forwarded_user_agent": null,
              "x_kong_proxy_latency": null,
              "x_kong_upstream_latency": null,
              "x_real_ip": "103.215.237.105"
            }
          ],
          "host": "vazjljkzryzrsxragumb.supabase.co",
          "method": "POST",
          "path": "/rest/v1/event_logs",
          "port": null,
          "protocol": "https:",
          "sb": [
            {
              "apikey": [],
              "auth_user": null,
              "jwt": [
                {
                  "apikey": [
                    {
                      "invalid": null,
                      "payload": [
                        {
                          "algorithm": "HS256",
                          "expires_at": 2089056039,
                          "issuer": "supabase",
                          "role": "anon",
                          "signature_prefix": "gNYdab",
                          "subject": null
                        }
                      ]
                    }
                  ],
                  "authorization": [
                    {
                      "invalid": null,
                      "payload": [
                        {
                          "algorithm": "HS256",
                          "expires_at": 2089056039,
                          "issuer": "supabase",
                          "key_id": null,
                          "role": "anon",
                          "session_id": null,
                          "signature_prefix": "gNYdab",
                          "subject": null
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ],
          "search": null,
          "url": "https://vazjljkzryzrsxragumb.supabase.co/rest/v1/event_logs"
        }
      ],
      "response": [
        {
          "headers": [
            {
              "cf_cache_status": "DYNAMIC",
              "cf_ray": "9ddc93147224a403-BLR",
              "content_length": null,
              "content_location": null,
              "content_range": null,
              "content_type": "application/json; charset=utf-8",
              "date": "Tue, 17 Mar 2026 14:09:18 GMT",
              "proxy_status": "PostgREST; error=22008",
              "sb_gateway_mode": null,
              "sb_gateway_version": "1",
              "sb_request_id": null,
              "transfer_encoding": "chunked",
              "x_kong_proxy_latency": null,
              "x_kong_upstream_latency": null,
              "x_sb_error_code": null
            }
          ],
          "origin_time": 575,
          "status_code": 400
        }
      ]
    }
  ],
  "timestamp": 1773756557442000
}
