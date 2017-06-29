package io.suggest.model

import java.util

import org.elasticsearch.action._
import org.elasticsearch.action.bulk.{BulkRequest, BulkRequestBuilder, BulkResponse}
import org.elasticsearch.action.delete.{DeleteRequest, DeleteRequestBuilder, DeleteResponse}
import org.elasticsearch.action.explain.{ExplainRequest, ExplainRequestBuilder, ExplainResponse}
import org.elasticsearch.action.fieldcaps.{FieldCapabilitiesRequest, FieldCapabilitiesRequestBuilder, FieldCapabilitiesResponse}
import org.elasticsearch.action.fieldstats.{FieldStatsRequest, FieldStatsRequestBuilder, FieldStatsResponse}
import org.elasticsearch.action.get._
import org.elasticsearch.action.index.{IndexRequest, IndexRequestBuilder, IndexResponse}
import org.elasticsearch.action.search._
import org.elasticsearch.action.termvectors._
import org.elasticsearch.action.update.{UpdateRequest, UpdateRequestBuilder, UpdateResponse}
import org.elasticsearch.client.{AdminClient, Client}
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.threadpool.ThreadPool

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.08.16 9:27
  * Description: Реализация ES-клиента Client, где всё незаимплеменчено.
  */
class MockedEsClient extends Client {
  override def prepareBulk(): BulkRequestBuilder = ???

  override def explain(request: ExplainRequest): ActionFuture[ExplainResponse] = ???

  override def explain(request: ExplainRequest, listener: ActionListener[ExplainResponse]): Unit = ???

  override def prepareClearScroll(): ClearScrollRequestBuilder = ???

  override def prepareUpdate(): UpdateRequestBuilder = ???

  override def prepareUpdate(index: String, `type`: String, id: String): UpdateRequestBuilder = ???

  override def prepareTermVector(): TermVectorsRequestBuilder = ???

  override def prepareTermVector(index: String, `type`: String, id: String): TermVectorsRequestBuilder = ???

  override def prepareDelete(): DeleteRequestBuilder = ???

  override def prepareDelete(index: String, `type`: String, id: String): DeleteRequestBuilder = ???

  override def fieldCaps(request: FieldCapabilitiesRequest): ActionFuture[FieldCapabilitiesResponse] = ???

  override def fieldCaps(request: FieldCapabilitiesRequest, listener: ActionListener[FieldCapabilitiesResponse]): Unit = ???

  override def settings(): Settings = ???

  override def multiGet(request: MultiGetRequest): ActionFuture[MultiGetResponse] = ???

  override def multiGet(request: MultiGetRequest, listener: ActionListener[MultiGetResponse]): Unit = ???

  override def filterWithHeader(headers: util.Map[String, String]): Client = ???

  override def searchScroll(request: SearchScrollRequest): ActionFuture[SearchResponse] = ???

  override def searchScroll(request: SearchScrollRequest, listener: ActionListener[SearchResponse]): Unit = ???

  override def prepareExplain(index: String, `type`: String, id: String): ExplainRequestBuilder = ???

  override def index(request: IndexRequest): ActionFuture[IndexResponse] = ???

  override def index(request: IndexRequest, listener: ActionListener[IndexResponse]): Unit = ???

  override def prepareMultiTermVectors(): MultiTermVectorsRequestBuilder = ???

  override def prepareMultiSearch(): MultiSearchRequestBuilder = ???

  override def bulk(request: BulkRequest): ActionFuture[BulkResponse] = ???

  override def bulk(request: BulkRequest, listener: ActionListener[BulkResponse]): Unit = ???

  override def prepareSearch(indices: String*): SearchRequestBuilder = ???

  override def admin(): AdminClient = ???

  override def prepareTermVectors(): TermVectorsRequestBuilder = ???

  override def prepareTermVectors(index: String, `type`: String, id: String): TermVectorsRequestBuilder = ???

  override def update(request: UpdateRequest): ActionFuture[UpdateResponse] = ???

  override def update(request: UpdateRequest, listener: ActionListener[UpdateResponse]): Unit = ???

  override def prepareFieldStats(): FieldStatsRequestBuilder = ???

  override def termVectors(request: TermVectorsRequest): ActionFuture[TermVectorsResponse] = ???

  override def termVectors(request: TermVectorsRequest, listener: ActionListener[TermVectorsResponse]): Unit = ???

  override def prepareSearchScroll(scrollId: String): SearchScrollRequestBuilder = ???

  override def delete(request: DeleteRequest): ActionFuture[DeleteResponse] = ???

  override def delete(request: DeleteRequest, listener: ActionListener[DeleteResponse]): Unit = ???

  override def search(request: SearchRequest): ActionFuture[SearchResponse] = ???

  override def search(request: SearchRequest, listener: ActionListener[SearchResponse]): Unit = ???

  override def get(request: GetRequest): ActionFuture[GetResponse] = ???

  override def get(request: GetRequest, listener: ActionListener[GetResponse]): Unit = ???

  override def termVector(request: TermVectorsRequest): ActionFuture[TermVectorsResponse] = ???

  override def termVector(request: TermVectorsRequest, listener: ActionListener[TermVectorsResponse]): Unit = ???

  override def prepareMultiGet(): MultiGetRequestBuilder = ???

  override def multiTermVectors(request: MultiTermVectorsRequest): ActionFuture[MultiTermVectorsResponse] = ???

  override def multiTermVectors(request: MultiTermVectorsRequest, listener: ActionListener[MultiTermVectorsResponse]): Unit = ???

  override def multiSearch(request: MultiSearchRequest): ActionFuture[MultiSearchResponse] = ???

  override def multiSearch(request: MultiSearchRequest, listener: ActionListener[MultiSearchResponse]): Unit = ???

  override def prepareGet(): GetRequestBuilder = ???

  override def prepareGet(index: String, `type`: String, id: String): GetRequestBuilder = ???

  override def prepareIndex(): IndexRequestBuilder = ???

  override def prepareIndex(index: String, `type`: String): IndexRequestBuilder = ???

  override def prepareIndex(index: String, `type`: String, id: String): IndexRequestBuilder = ???

  override def clearScroll(request: ClearScrollRequest): ActionFuture[ClearScrollResponse] = ???

  override def clearScroll(request: ClearScrollRequest, listener: ActionListener[ClearScrollResponse]): Unit = ???

  override def prepareFieldCaps(): FieldCapabilitiesRequestBuilder = ???

  override def fieldStats(request: FieldStatsRequest): ActionFuture[FieldStatsResponse] = ???

  override def fieldStats(request: FieldStatsRequest, listener: ActionListener[FieldStatsResponse]): Unit = ???

  override def threadPool(): ThreadPool = ???

  override def execute[Request <: ActionRequest, Response <: ActionResponse, RequestBuilder <: ActionRequestBuilder[Request, Response, RequestBuilder]](action: Action[Request, Response, RequestBuilder], request: Request): ActionFuture[Response] = ???

  override def execute[Request <: ActionRequest, Response <: ActionResponse, RequestBuilder <: ActionRequestBuilder[Request, Response, RequestBuilder]](action: Action[Request, Response, RequestBuilder], request: Request, listener: ActionListener[Response]): Unit = ???

  override def prepareExecute[Request <: ActionRequest, Response <: ActionResponse, RequestBuilder <: ActionRequestBuilder[Request, Response, RequestBuilder]](action: Action[Request, Response, RequestBuilder]): RequestBuilder = ???

  override def close(): Unit = ???
}
