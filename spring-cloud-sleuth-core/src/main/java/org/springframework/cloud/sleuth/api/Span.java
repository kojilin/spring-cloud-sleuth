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

package org.springframework.cloud.sleuth.api;

import org.springframework.lang.Nullable;

public interface Span extends SpanCustomizer {

	/**
	 * When true, no recording is done and nothing is reported to zipkin. However, this
	 * span should still be injected into outgoing requests. Use this flag to avoid
	 * performing expensive computation.
	 */
	boolean isNoop();

	TraceContext context();

	/**
	 * Starts the span with an implicit timestamp.
	 *
	 * <p>
	 * Spans can be modified before calling start. For example, you can add tags to the
	 * span and set its name without lock contention.
	 */
	Span start();

	/**
	 * Like {@link #start()}, except with a given timestamp in microseconds.
	 *
	 * <p>
	 * Take extreme care with this feature as it is easy to have incorrect timestamps. If
	 * you must use this, generate the timestamp using
	 */
	Span start(long timestamp);

	/** {@inheritDoc} */
	Span name(String name);

	/**
	 * When present, the span is remote. This value clarifies how to interpret
	 * {@link #remoteServiceName(String)} and {@link #remoteIpAndPort(String, int)}.
	 *
	 * <p>
	 * Note: This affects Zipkin v1 format even if that format does not have a "kind"
	 * field. For example, if kind is {@link Span.Kind#SERVER} and reported in v1 Zipkin
	 * format, the span's start timestamp is implicitly annotated as "sr" and that plus
	 * its duration as "ss".
	 */
	Span kind(@Nullable Span.Kind kind);

	/** {@inheritDoc} */
	Span annotate(String value);

	/**
	 * Like {@link #annotate(String)}, except with a given timestamp in microseconds.
	 *
	 * <p>
	 * Take extreme care with this feature as it is easy to have incorrect timestamps.
	 */
	Span annotate(long timestamp, String value);

	/** {@inheritDoc} */
	Span tag(String key, String value);

	/**
	 * Records an error that impacted this operation.
	 *
	 * <p>
	 * <em>Note:</em> Calling this does not {@linkplain #finish() finish} the span.
	 *
	 * @since 4.19
	 */
	// Design note: <T extends Throwable> T error(T throwable) is tempting but this
	// doesn't work in
	// multi-catch. In practice, you should always at least catch RuntimeException and
	// Error.
	Span error(Throwable throwable);

	/**
	 * Lower-case label of the remote node in the service graph, such as "favstar". Do not
	 * set if unknown. Avoid names with variables or unique identifiers embedded.
	 *
	 * <p>
	 * This is a primary label for trace lookup and aggregation, so it should be intuitive
	 * and consistent. Many use a name from service discovery.
	 *
	 * @see #remoteIpAndPort(String, int)
	 */
	Span remoteServiceName(String remoteServiceName);

	/**
	 * Sets the IP and port associated with the remote endpoint. For example, the server's
	 * listen socket or the connected client socket. This can also be set to forwarded
	 * values, such as an advertised IP.
	 *
	 * <p>
	 * Invalid inputs, such as hostnames, will return false. Port is only set with a valid
	 * IP, and zero or negative port values are ignored. For example, to set only the IP
	 * address, leave port as zero.
	 *
	 * <p>
	 * This returns boolean, not Span as it is often the case strings are malformed. Using
	 * this, you can do conditional parsing like so: <pre>{@code
	 * if (span.remoteIpAndPort(address.getHostAddress(), target.getPort())) return;
	 * span.remoteIpAndPort(address.getHostName(), target.getPort());
	 * }</pre>
	 *
	 * <p>
	 * Note: Comma separated lists are not supported. If you have multiple entries choose
	 * the one most indicative of the remote side. For example, the left-most entry in
	 * X-Forwarded-For.
	 * @param remoteIp the IPv4 or IPv6 literal representing the remote service connection
	 * @param remotePort the port associated with the IP, or zero if unknown.
	 * @see #remoteServiceName(String)
	 * @since 5.2
	 */
	// NOTE: this is remote (IP, port) vs remote IP:port String as zipkin2.Endpoint
	// separates the two,
	// and IP:port strings are uncommon at runtime (even if they are common at config).
	// Parsing IP:port pairs on each request, including concerns like IPv6 bracketing,
	// would add
	// weight for little benefit. If this changes, we can overload it.
	Span remoteIpAndPort(@Nullable String remoteIp, int remotePort);

	/** Reports the span complete, assigning the most precise duration possible. */
	void finish();

	/** Throws away the current span without reporting it. */
	void abandon();

	/**
	 * Like {@link #finish()}, except with a given timestamp in microseconds.
	 *
	 * <p>
	 * {link zipkin2.Span#duration() Zipkin's span duration} is derived by subtracting the
	 * start timestamp from this, and set when appropriate.
	 *
	 * <p>
	 * Take extreme care with this feature as it is easy to have incorrect timestamps. If
	 * you must use this, generate the timestamp using {link Tracing#clock(TraceContext)}.
	 */
	// Design note: This differs from Brave 3's LocalTracer which completes with a given
	// duration.
	// This was changed for a few use cases.
	// * Finishing a one-way span on another host
	// https://github.com/openzipkin/zipkin/issues/1243
	// * The other host will not be able to read the start timestamp, so can't calculate
	// duration
	// * Consistency in Api: All units and measures are epoch microseconds
	// * This can reduce accidents where people use duration when they mean a timestamp
	// * Parity with OpenTracing
	// * OpenTracing close spans like this, and this makes a Brave bridge stateless wrt
	// timestamps
	// Design note: This does not implement Closeable (or AutoCloseable)
	// * the try-with-resources pattern is be reserved for attaching a span to a context.
	void finish(long timestamp);

	/**
	 * Reports the span, even if unfinished. Most users will not call this method.
	 *
	 * <p>
	 * This primarily supports two use cases: one-way spans and orphaned spans. For
	 * example, a one-way span can be modeled as a span where one tracer calls start and
	 * another calls finish. In order to report that span from its origin, flush must be
	 * called.
	 *
	 * <p>
	 * Another example is where a user didn't call finish within a deadline or before a
	 * shutdown occurs. By flushing, you can report what was in progress.
	 */
	// Design note: This does not implement Flushable
	// * a span should not be routinely flushed, only when it has finished, or we don't
	// believe this
	// tracer will finish it.
	void flush();

	enum Kind {

		CLIENT, SERVER,
		/**
		 * When present, {@link #start()} is the moment a producer sent a message to a
		 * destination. A duration between {@link #start()} and {@link #finish()} may
		 * imply batching delay.
		 *
		 * <p>
		 * Unlike {@link #CLIENT}, messaging spans never share a span ID. For example, the
		 * {@link #CONSUMER} of the same message has {@link TraceContext#parentId()} set
		 * to this span's {@link TraceContext#spanId()}.
		 */
		PRODUCER,
		/**
		 * When present, {@link #start()} is the moment a consumer received a message from
		 * an origin. A duration between {@link #start()} and {@link #finish()} may imply
		 * a processing backlog.
		 *
		 * <p>
		 * Unlike {@link #SERVER}, messaging spans never share a span ID. For example, the
		 * {@link #PRODUCER} of this message is the {@link TraceContext#parentId()} of
		 * this span.
		 */
		CONSUMER

	}

}