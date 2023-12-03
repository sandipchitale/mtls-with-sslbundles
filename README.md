# Description

A simple Spring Boot application showing how to use multiple SslBundles, including a wrapper for JDK `cacerts` to
support Mutual TLS using:

- `RestClient`
- `RestTemplate`
- `WebClient`

This is in reference to the issue [38387](https://github.com/spring-projects/spring-boot/issues/38387).

The key idea is to create the low level SslContext using the `KeyManager`s and `TrustManager`s from the 
`SslBundle`. The `TrustManager`s are wrapped in a composite `TrustManager` that delegates to set of `TrustManagers`. 
