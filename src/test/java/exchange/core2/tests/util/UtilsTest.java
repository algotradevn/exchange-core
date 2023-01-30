package exchange.core2.tests.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import exchange.core2.core.utils.Utils;

public class UtilsTest {

	@Test
	public void testCheckSubArrayEqualsTotal() {
		final List<Long> subArray2 = Utils
				.getFirstMatchedSubArray(Lists.newArrayList(4L, 1L, 6L, 5L, 10L), 10L);
		Assertions.assertTrue(subArray2 != null && subArray2.size() > 0);
		
		final List<Long> subArray3 = Utils
				.getFirstMatchedSubArray(Lists.newArrayList(1L, 3L, 1L, 5L), 5L);
		Assertions.assertTrue(subArray3 != null && subArray3.size() > 0);
	}

	@Test
	public void testCheckSubArrayEqualsTotal2() {
		// final ArrayList<Long> values = Lists.newArrayList(10L, 5L, 6L, 1L,
		// 4L);
		// final ArrayList<Long> values = Lists.newArrayList(3L, 2L, 4L, 5L);
		// final ArrayList<Long> values = Lists.newArrayList(4L, 1L, 6L, 5L,
		// 10L);
		final ArrayList<Long> values = Lists.newArrayList(3L, 4L, 5L, 2L);
		final List<Long> subArray2 = Utils.getSubsetSum(values, values.size(), 9L, true);
		Assertions.assertTrue(subArray2 != null && subArray2.size() > 0);
	}
}
