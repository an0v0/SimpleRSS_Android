package jp.hatenablog.an.simplerss

interface RSSConnectorCallback<T> {

    fun didFailedWithError(connector: RSSConnector, exception: Exception?)

    fun connectorDidFinishDownloading(connector: RSSConnector)
}
