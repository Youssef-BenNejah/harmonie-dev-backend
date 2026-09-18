package com.harmoniedev.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.harmoniedev.api.config.SecurityProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {
	private ClientIpResolver resolver(String... trusted) {
		SecurityProperties props = new SecurityProperties();
		props.setTrustedProxies(List.of(trusted));
		return new ClientIpResolver(props);
	}

	private MockHttpServletRequest request(String peer, String forwarded) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr(peer);
		if (forwarded != null) {
			request.addHeader("X-Forwarded-For", forwarded);
		}
		return request;
	}

	@Test
	void noTrustedProxies_headerIsIgnoredSoRotatingItCannotBypassTheLimit() {
		ClientIpResolver r = resolver();
		assertThat(r.resolve(request("203.0.113.9", "1.1.1.1"))).isEqualTo("203.0.113.9");
		assertThat(r.resolve(request("203.0.113.9", "2.2.2.2"))).isEqualTo("203.0.113.9");
	}

	@Test
	void untrustedPeer_headerIsIgnoredEvenWhenProxiesAreConfigured() {
		assertThat(resolver("10.0.0.0/8").resolve(request("203.0.113.9", "1.1.1.1"))).isEqualTo("203.0.113.9");
	}

	@Test
	void trustedProxy_usesTheClientItSaw() {
		assertThat(resolver("10.0.0.5").resolve(request("10.0.0.5", "198.51.100.7"))).isEqualTo("198.51.100.7");
	}

	@Test
	void trustedProxy_clientCannotForgeTheLeftmostEntry() {
		// client sent "1.1.1.1"; the proxy appended the address it actually saw
		assertThat(resolver("10.0.0.5").resolve(request("10.0.0.5", "1.1.1.1, 198.51.100.7"))).isEqualTo("198.51.100.7");
	}

	@Test
	void proxyChain_skipsEveryTrustedHopFromTheRight() {
		assertThat(resolver("10.0.0.0/8").resolve(request("10.0.0.5", "9.9.9.9, 198.51.100.7, 10.1.2.3"))).isEqualTo("198.51.100.7");
	}

	@Test
	void garbageInTheHeader_fallsBackToThePeerWithoutResolvingNames() {
		assertThat(resolver("10.0.0.5").resolve(request("10.0.0.5", "evil.example.com"))).isEqualTo("10.0.0.5");
		assertThat(resolver("10.0.0.5").resolve(request("10.0.0.5", "not an ip"))).isEqualTo("10.0.0.5");
	}

	@Test
	void trustedProxyWithoutHeader_usesThePeer() {
		assertThat(resolver("10.0.0.5").resolve(request("10.0.0.5", null))).isEqualTo("10.0.0.5");
	}
}
