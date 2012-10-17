GuaranteedTimeoutConnection
===========================
<img src="http://mindthis.ca/wp-content/uploads/2011/12/timeout.jpg"/>

A wrapper class around HttpURLConnection that guarantees a specific timeout. 
<br><br>
<b>Example</b><br>
GuaranteedTimeoutConnection gtc = new GuaranteedTimeoutConnection("Download123", 30000, false, null);<br>
InputStreamCallback inputStreamCallback = new InputStreamCallback()<br>
{
<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; @Override<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; public void getInputStream(InputStream inputStream, Exception exception)<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; {<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; }<br>
};<br>
gtc.getInputStream(inputStreamCallback, new URL("http://www.google.com", false);<br><br>
The constructor takes three params:
<br>
1) Timeout; an integer values in milliseconds.<br>
2) useSSL; a boolean that will either utilize HttpURLConnection or HttpsURLConnection respectively.<br>
3) uiHandler; pass null and the constructor will create a handler, or pass in a handler to the ui if available.
<br><br>
There's 4 public methods:
<br>
1) A getter to retrieve the HttpURLConnection reference.
<br>
2) A getter to retrieve the HttpsURLConnection reference.
<br>
3) A convenient method to get an inputStream on a supplied URL. Provide an InputStreamCallback and the URL to connect to. If an input stream
is not returned within the specified timeut a standard SocketException/IOException gets returned in the callback.
<br>
4) A convenient method to open the connection. Supply OpenConnectionCallback and the URL to connect to. If a connection is not opened 
within the specified timeout a standard SocketException/IOException gets returned in the callback.
<br><br>
After the callback is hit in the openConnection method, you'll likely want a reference to the URLConnection which can be retrieved using one of the available public getters.<br><br>

<b>Warnings</b><br>
Be cautios that the constructor creates a handler if not provided one! If you pass null as the uiHandler param make sure you either call the constructor from the UI thread, or use Looper properly.

<B> COPYRIGHT </B>

Copyright 2012 Noah Seidman

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.