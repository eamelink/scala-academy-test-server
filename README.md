Sample Exchange Rate Server
===========================

This is a sample server application that serves sample exchange rates. It's intended to allow you to write a test client and experiment with combining various WS responses, selecting the quickest one, etcetera.


If has the following methods:

/failfast
---
This request fails immediately, with status code 500 and `text/plain` content type.

/failslow
---
This request fails after 3 seconds, with status code 500 and `text/plain` content type.

/exchange1
---
When it works, responds with a status code 200 and `text/plain` content.

Sometimes it works fast, sometimes it is slow (~3 seconds) and sometimes it fails.

If it fails, the status code is 500, the content type unspecified.

/exchange2
---
When it works, responds with a status code 200 and `application/json` content.

Sometimes it works fast, sometimes it is slow (~3 seconds) and sometimes it fails.

If it fails, the status code is 500, the content type unspecified.

/exchange3
---
When it works, responds with a status code 200 and `application/json` content.

Sometimes it works *reasonably* fast (~50 ms), sometimes it is slow (~3 seconds).

/chunked
---
Returns a *chunked* HTTP response, status 200 with content type `application/json`.

Each chunk is a valid JSON structure.

/stream
---
Serves a WebSocket connection which streams `application/json` chunks. 

Response code is 200 when the request is a WebSocket request, and 400 (BadRequest) when the request is not a WebSocket request.
