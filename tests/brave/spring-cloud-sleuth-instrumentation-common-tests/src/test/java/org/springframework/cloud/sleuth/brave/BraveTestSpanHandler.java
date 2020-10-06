/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.brave;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import brave.handler.MutableSpan;

import org.springframework.cloud.sleuth.api.Span;
import org.springframework.cloud.sleuth.test.ReportedSpan;
import org.springframework.cloud.sleuth.test.TestSpanHandler;

public class BraveTestSpanHandler implements TestSpanHandler {

	final brave.test.TestSpanHandler spans;

	public BraveTestSpanHandler(brave.test.TestSpanHandler spans) {
		this.spans = spans;
	}

	@Override
	public List<ReportedSpan> reportedSpans() {
		return this.spans.spans().stream().map(FromMutableSpan::new).collect(Collectors.toList());
	}

	@Override
	public ReportedSpan takeLocalSpan() {
		// TODO: [OTEL] fix me
		return null;
	}

	@Override
	public void clear() {
		this.spans.clear();
	}

	@Override
	public ReportedSpan takeRemoteSpan(Span.Kind kind) {
		// TODO: [OTEL] fix me
		return null;
	}

	@Override
	public ReportedSpan takeRemoteSpanWithError(Span.Kind kind) {
		// TODO: [OTEL] fix me
		return null;
	}

	@Override
	public ReportedSpan get(int index) {
		return new FromMutableSpan(this.spans.get(index));
	}

	@Override
	public Iterator<ReportedSpan> iterator() {
		return reportedSpans().iterator();
	}

}

class FromMutableSpan implements ReportedSpan {

	final MutableSpan mutableSpan;

	FromMutableSpan(MutableSpan mutableSpan) {
		this.mutableSpan = mutableSpan;
	}

	@Override
	public String name() {
		return this.mutableSpan.name();
	}

	@Override
	public long finishTimestamp() {
		return this.mutableSpan.finishTimestamp();
	}

	@Override
	public Map<String, String> tags() {
		return this.mutableSpan.tags();
	}

	@Override
	public String id() {
		return this.mutableSpan.id();
	}

	@Override
	public String parentId() {
		return this.mutableSpan.parentId();
	}

	@Override
	public String remoteIp() {
		return this.mutableSpan.remoteIp();
	}

	@Override
	public int remotePort() {
		return this.mutableSpan.remotePort();
	}

	@Override
	public String traceId() {
		return this.mutableSpan.traceId();
	}

	@Override
	public Throwable error() {
		return this.mutableSpan.error();
	}

	@Override
	public Span.Kind kind() {
		return Span.Kind.valueOf(this.mutableSpan.kind().name());
	}

}