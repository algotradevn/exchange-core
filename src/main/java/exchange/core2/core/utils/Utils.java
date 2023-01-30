/*
 * Copyright 2019 Maksim Zheravin
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
package exchange.core2.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Utils {

	public static List<Long> getFirstMatchedSubArray(final List<Long> values, final long total) {

		long curSum = values.get(0);
		int start = 0;
		final int size = values.size();
		for (int i = 1; i <= size; i++) {
			while (curSum > total && start < i - 1) {
				curSum -= values.get(start);
				start++;
			}

			if (curSum == total) {
				final int end = i - 1;
				if (start == end) {
					return new ArrayList<Long>() {
						private static final long serialVersionUID = 5365452686683929116L;
						{
							add(values.get(end));
						}
					};
				}
				return values.subList(start, end + 1); // check subList doc
			}

			if (i < size) {
				curSum += values.get(i);
			}
		}

		return Collections.emptyList();
	}

	public static List<Long> getSubsetSum(final List<Long> values, final int n, final long sum,
			final boolean addLatest) {
		final List<Long> result = new ArrayList<Long>();
		if (sum == 0L) {
			result.add(values.get(n));
			return result;
		}
		if (n == 0 || sum < 0) {
			return Collections.emptyList();
		}
		final Long lastValue = values.get(n - 1);
		if (lastValue > sum) {
			result.addAll(getSubsetSum(values, n - 1, sum, false));
			return result;
		}
		final long nextSum = sum - lastValue;
		final List<Long> subsetN1Sum = getSubsetSum(values, n - 1, nextSum, true);
		if (!isEmptyList(subsetN1Sum)) {
			result.addAll(subsetN1Sum);
			if (n > 0 && addLatest && nextSum != 0L) {
				result.add(values.get(n - 1));
			}
		} else {
			result.addAll(getSubsetSum(values, n - 1, sum, true));
		}
		return result;
	}

	private static <T extends Object> boolean isEmptyList(final Collection<T> list) {
		return list == null || list.size() == 0;
	}
}
