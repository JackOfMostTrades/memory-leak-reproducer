This test demonstrates a memory leak occurring with gRPC/Netty when using the jetty-alpn
SSL provider. Running this test assumes that both 8980 and 16000 are free ports on the
local system. This test runs in a non-terminating loop and prints the number of objects
in the ALPN.objects map. This number will grow indefinitely the longer the program runs.

Run the demo with:

    ./gradlew runDemo
