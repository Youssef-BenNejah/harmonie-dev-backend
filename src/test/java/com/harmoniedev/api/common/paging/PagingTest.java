package com.harmoniedev.api.common.paging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

class PagingTest {
	@Test
	void size_isClampedSoOneRequestCannotPullTheWholeCollection() {
		assertThat(Paging.of(0, 10_000).getPageSize()).isEqualTo(Paging.MAX_SIZE);
		assertThat(Paging.of(0, 0).getPageSize()).isEqualTo(Paging.DEFAULT_SIZE);
		assertThat(Paging.of(0, -5).getPageSize()).isEqualTo(Paging.DEFAULT_SIZE);
	}

	@Test
	void negativePage_becomesFirstPage() {
		assertThat(Paging.of(-3, 20).getPageNumber()).isZero();
	}

	@Test
	void order_isStableByIdSoPagesNeverOverlap() {
		assertThat(Paging.of(2, 20).getSort().getOrderFor("id")).isNotNull();
	}

	@Test
	void response_carriesTotalsInHeadersAndKeepsTheArrayBody() {
		var page = new PageImpl<>(List.of("a", "b"), Paging.of(1, 2), 5);
		var response = Paging.respond("Things", page);
		assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("5");
		assertThat(response.getHeaders().getFirst("X-Total-Pages")).isEqualTo("3");
		assertThat(response.getHeaders().getFirst("X-Page")).isEqualTo("1");
		assertThat(response.getBody().getData()).containsExactly("a", "b");
	}
}
