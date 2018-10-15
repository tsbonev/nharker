package com.tsbonev.nharker.server.helpers

/**
 * Http status codes taken from ktor's status code class to
 * decouple the responses from the framework completely.
 *
 * @author Tsvetozar Bonev (tsbonev@gmail.com)
 */
@Suppress("unused")
data class HttpStatus(val value: Int, val description: String) {
    companion object {
        val Continue = HttpStatus(100, "Continue")
        val SwitchingProtocols = HttpStatus(101, "Switching Protocols")
        val Processing = HttpStatus(102, "Processing")

        val OK = HttpStatus(200, "OK")
        val Created = HttpStatus(201, "Created")
        val Accepted = HttpStatus(202, "Accepted")
        val NonAuthoritativeInformation = HttpStatus(203, "Non-Authoritative Information")
        val NoContent = HttpStatus(204, "No Content")
        val ResetContent = HttpStatus(205, "Reset Content")
        val PartialContent = HttpStatus(206, "Partial Content")
        val MultiStatus = HttpStatus(207, "Multi-Status")

        val MultipleChoices = HttpStatus(300, "Multiple Choices")
        val MovedPermanently = HttpStatus(301, "Moved Permanently")
        val Found = HttpStatus(302, "Found")
        val SeeOther = HttpStatus(303, "See Other")
        val NotModified = HttpStatus(304, "Not Modified")
        val UseProxy = HttpStatus(305, "Use Proxy")
        val SwitchProxy = HttpStatus(306, "Switch Proxy")
        val TemporaryRedirect = HttpStatus(307, "Temporary Redirect")
        val PermanentRedirect = HttpStatus(308, "Permanent Redirect")

        val BadRequest = HttpStatus(400, "Bad Request")
        val Unauthorized = HttpStatus(401, "Unauthorized")
        val PaymentRequired = HttpStatus(402, "Payment Required")
        val Forbidden = HttpStatus(403, "Forbidden")
        val NotFound = HttpStatus(404, "Not Found")
        val MethodNotAllowed = HttpStatus(405, "Method Not Allowed")
        val NotAcceptable = HttpStatus(406, "Not Acceptable")
        val ProxyAuthenticationRequired = HttpStatus(407, "Proxy Authentication Required")
        val RequestTimeout = HttpStatus(408, "Request Timeout")
        val Conflict = HttpStatus(409, "Conflict")
        val Gone = HttpStatus(410, "Gone")
        val LengthRequired = HttpStatus(411, "Length Required")
        val PreconditionFailed = HttpStatus(412, "Precondition Failed")
        val PayloadTooLarge = HttpStatus(413, "Payload Too Large")
        val RequestURITooLong = HttpStatus(414, "Request-URI Too Long")

        val UnsupportedMediaType = HttpStatus(415, "Unsupported Media Type")
        val RequestedRangeNotSatisfiable = HttpStatus(416, "Requested Range Not Satisfiable")
        val ExpectationFailed = HttpStatus(417, "Expectation Failed")
        val UnprocessableEntity = HttpStatus(422, "Unprocessable Entity")
        val Locked = HttpStatus(423, "Locked")
        val FailedDependency = HttpStatus(424, "Failed Dependency")
        val UpgradeRequired = HttpStatus(426, "Upgrade Required")
        val TooManyRequests = HttpStatus(429, "Too Many Requests")
        val RequestHeaderFieldTooLarge = HttpStatus(431, "Request Header Fields Too Large")

        val InternalServerError = HttpStatus(500, "Internal Server Error")
        val NotImplemented = HttpStatus(501, "Not Implemented")
        val BadGateway = HttpStatus(502, "Bad Gateway")
        val ServiceUnavailable = HttpStatus(503, "Service Unavailable")
        val GatewayTimeout = HttpStatus(504, "Gateway Timeout")
        val VersionNotSupported = HttpStatus(505, "HTTP Version Not Supported")
        val VariantAlsoNegotiates = HttpStatus(506, "Variant Also Negotiates")
        val InsufficientStorage = HttpStatus(507, "Insufficient Storage")
    }
}

@Suppress("unused")
fun HttpStatus.isSuccess(): Boolean = value in (200 until 300)

@Suppress("unused")
fun HttpStatus.isUserError(): Boolean = value in (400 until 500)

@Suppress("unused")
fun HttpStatus.isInternalError(): Boolean = value in (500 until 600)