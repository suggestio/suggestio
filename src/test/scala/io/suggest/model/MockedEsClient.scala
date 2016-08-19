package io.suggest.model

import org.elasticsearch.action._
import org.elasticsearch.action.bulk.{BulkRequest, BulkRequestBuilder, BulkResponse}
import org.elasticsearch.action.count.{CountRequest, CountRequestBuilder, CountResponse}
import org.elasticsearch.action.delete.{DeleteRequest, DeleteRequestBuilder, DeleteResponse}
import org.elasticsearch.action.deletebyquery.{DeleteByQueryRequest, DeleteByQueryRequestBuilder, DeleteByQueryResponse}
import org.elasticsearch.action.exists.{ExistsRequest, ExistsRequestBuilder, ExistsResponse}
import org.elasticsearch.action.explain.{ExplainRequest, ExplainRequestBuilder, ExplainResponse}
import org.elasticsearch.action.fieldstats.{FieldStatsRequest, FieldStatsRequestBuilder, FieldStatsResponse}
import org.elasticsearch.action.get._
import org.elasticsearch.action.index.{IndexRequest, IndexRequestBuilder, IndexResponse}
import org.elasticsearch.action.indexedscripts.delete.{DeleteIndexedScriptRequest, DeleteIndexedScriptRequestBuilder, DeleteIndexedScriptResponse}
import org.elasticsearch.action.indexedscripts.get.{GetIndexedScriptRequest, GetIndexedScriptRequestBuilder, GetIndexedScriptResponse}
import org.elasticsearch.action.indexedscripts.put.{PutIndexedScriptRequest, PutIndexedScriptRequestBuilder, PutIndexedScriptResponse}
import org.elasticsearch.action.mlt.{MoreLikeThisRequest, MoreLikeThisRequestBuilder}
import org.elasticsearch.action.percolate._
import org.elasticsearch.action.search._
import org.elasticsearch.action.suggest.{SuggestRequest, SuggestRequestBuilder, SuggestResponse}
import org.elasticsearch.action.termvector._
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

  override def prepareExists(indices: String*): ExistsRequestBuilder = ???

  override def prepareDeleteIndexedScript(): DeleteIndexedScriptRequestBuilder = ???

  override def prepareDeleteIndexedScript(scriptLang: String, id: String): DeleteIndexedScriptRequestBuilder = ???

  override def suggest(request: SuggestRequest): ActionFuture[SuggestResponse] = ???

  override def suggest(request: SuggestRequest, listener: ActionListener[SuggestResponse]): Unit = ???

  override def count(request: CountRequest): ActionFuture[CountResponse] = ???

  override def count(request: CountRequest, listener: ActionListener[CountResponse]): Unit = ???

  override def prepareClearScroll(): ClearScrollRequestBuilder = ???

  override def prepareGet(): GetRequestBuilder = ???

  override def prepareGet(index: String, `type`: String, id: String): GetRequestBuilder = ???

  override def deleteByQuery(request: DeleteByQueryRequest): ActionFuture[DeleteByQueryResponse] = ???

  override def deleteByQuery(request: DeleteByQueryRequest, listener: ActionListener[DeleteByQueryResponse]): Unit = ???

  override def multiPercolate(request: MultiPercolateRequest): ActionFuture[MultiPercolateResponse] = ???

  override def multiPercolate(request: MultiPercolateRequest, listener: ActionListener[MultiPercolateResponse]): Unit = ???

  override def prepareGetIndexedScript(): GetIndexedScriptRequestBuilder = ???

  override def prepareGetIndexedScript(scriptLang: String, id: String): GetIndexedScriptRequestBuilder = ???

  override def update(request: UpdateRequest): ActionFuture[UpdateResponse] = ???

  override def update(request: UpdateRequest, listener: ActionListener[UpdateResponse]): Unit = ???

  override def termVector(request: TermVectorRequest): ActionFuture[TermVectorResponse] = ???

  override def termVector(request: TermVectorRequest, listener: ActionListener[TermVectorResponse]): Unit = ???

  override def deleteIndexedScript(request: DeleteIndexedScriptRequest, listener: ActionListener[DeleteIndexedScriptResponse]): Unit = ???

  override def deleteIndexedScript(request: DeleteIndexedScriptRequest): ActionFuture[DeleteIndexedScriptResponse] = ???

  override def preparePutIndexedScript(): PutIndexedScriptRequestBuilder = ???

  override def preparePutIndexedScript(scriptLang: String, id: String, source: String): PutIndexedScriptRequestBuilder = ???

  override def prepareMultiTermVectors(): MultiTermVectorsRequestBuilder = ???

  override def get(request: GetRequest): ActionFuture[GetResponse] = ???

  override def get(request: GetRequest, listener: ActionListener[GetResponse]): Unit = ???

  override def prepareUpdate(): UpdateRequestBuilder = ???

  override def prepareUpdate(index: String, `type`: String, id: String): UpdateRequestBuilder = ???

  override def prepareExplain(index: String, `type`: String, id: String): ExplainRequestBuilder = ???

  override def percolate(request: PercolateRequest): ActionFuture[PercolateResponse] = ???

  override def percolate(request: PercolateRequest, listener: ActionListener[PercolateResponse]): Unit = ???

  override def bulk(request: BulkRequest): ActionFuture[BulkResponse] = ???

  override def bulk(request: BulkRequest, listener: ActionListener[BulkResponse]): Unit = ???

  override def prepareSearchScroll(scrollId: String): SearchScrollRequestBuilder = ???

  override def getIndexedScript(request: GetIndexedScriptRequest, listener: ActionListener[GetIndexedScriptResponse]): Unit = ???

  override def getIndexedScript(request: GetIndexedScriptRequest): ActionFuture[GetIndexedScriptResponse] = ???

  override def multiGet(request: MultiGetRequest): ActionFuture[MultiGetResponse] = ???

  override def multiGet(request: MultiGetRequest, listener: ActionListener[MultiGetResponse]): Unit = ???

  override def prepareMultiGet(): MultiGetRequestBuilder = ???

  override def explain(request: ExplainRequest): ActionFuture[ExplainResponse] = ???

  override def explain(request: ExplainRequest, listener: ActionListener[ExplainResponse]): Unit = ???

  override def admin(): AdminClient = ???

  override def prepareFieldStats(): FieldStatsRequestBuilder = ???

  override def moreLikeThis(request: MoreLikeThisRequest): ActionFuture[SearchResponse] = ???

  override def moreLikeThis(request: MoreLikeThisRequest, listener: ActionListener[SearchResponse]): Unit = ???

  override def delete(request: DeleteRequest): ActionFuture[DeleteResponse] = ???

  override def delete(request: DeleteRequest, listener: ActionListener[DeleteResponse]): Unit = ???

  override def multiTermVectors(request: MultiTermVectorsRequest): ActionFuture[MultiTermVectorsResponse] = ???

  override def multiTermVectors(request: MultiTermVectorsRequest, listener: ActionListener[MultiTermVectorsResponse]): Unit = ???

  override def preparePercolate(): PercolateRequestBuilder = ???

  override def putIndexedScript(request: PutIndexedScriptRequest, listener: ActionListener[PutIndexedScriptResponse]): Unit = ???

  override def putIndexedScript(request: PutIndexedScriptRequest): ActionFuture[PutIndexedScriptResponse] = ???

  override def fieldStats(request: FieldStatsRequest): ActionFuture[FieldStatsResponse] = ???

  override def fieldStats(request: FieldStatsRequest, listener: ActionListener[FieldStatsResponse]): Unit = ???

  override def prepareIndex(): IndexRequestBuilder = ???

  override def prepareIndex(index: String, `type`: String): IndexRequestBuilder = ???

  override def prepareIndex(index: String, `type`: String, id: String): IndexRequestBuilder = ???

  override def prepareDeleteByQuery(indices: String*): DeleteByQueryRequestBuilder = ???

  override def prepareMultiPercolate(): MultiPercolateRequestBuilder = ???

  override def index(request: IndexRequest): ActionFuture[IndexResponse] = ???

  override def index(request: IndexRequest, listener: ActionListener[IndexResponse]): Unit = ???

  override def prepareDelete(): DeleteRequestBuilder = ???

  override def prepareDelete(index: String, `type`: String, id: String): DeleteRequestBuilder = ???

  override def searchScroll(request: SearchScrollRequest): ActionFuture[SearchResponse] = ???

  override def searchScroll(request: SearchScrollRequest, listener: ActionListener[SearchResponse]): Unit = ???

  override def prepareBulk(): BulkRequestBuilder = ???

  override def prepareMultiSearch(): MultiSearchRequestBuilder = ???

  override def prepareSuggest(indices: String*): SuggestRequestBuilder = ???

  override def settings(): Settings = ???

  override def multiSearch(request: MultiSearchRequest): ActionFuture[MultiSearchResponse] = ???

  override def multiSearch(request: MultiSearchRequest, listener: ActionListener[MultiSearchResponse]): Unit = ???

  override def prepareTermVector(): TermVectorRequestBuilder = ???

  override def prepareTermVector(index: String, `type`: String, id: String): TermVectorRequestBuilder = ???

  override def prepareSearch(indices: String*): SearchRequestBuilder = ???

  override def clearScroll(request: ClearScrollRequest): ActionFuture[ClearScrollResponse] = ???

  override def clearScroll(request: ClearScrollRequest, listener: ActionListener[ClearScrollResponse]): Unit = ???

  override def exists(request: ExistsRequest): ActionFuture[ExistsResponse] = ???

  override def exists(request: ExistsRequest, listener: ActionListener[ExistsResponse]): Unit = ???

  override def search(request: SearchRequest): ActionFuture[SearchResponse] = ???

  override def search(request: SearchRequest, listener: ActionListener[SearchResponse]): Unit = ???

  override def prepareCount(indices: String*): CountRequestBuilder = ???

  override def prepareMoreLikeThis(index: String, `type`: String, id: String): MoreLikeThisRequestBuilder = ???

  override def prepareExecute[Request <: ActionRequest[_], Response <: ActionResponse, RequestBuilder <: ActionRequestBuilder[Request, Response, RequestBuilder, Client]](action: Action[Request, Response, RequestBuilder, Client]): RequestBuilder = ???

  override def execute[Request <: ActionRequest[_], Response <: ActionResponse, RequestBuilder <: ActionRequestBuilder[Request, Response, RequestBuilder, Client]](action: Action[Request, Response, RequestBuilder, Client], request: Request): ActionFuture[Response] = ???

  override def execute[Request <: ActionRequest[_], Response <: ActionResponse, RequestBuilder <: ActionRequestBuilder[Request, Response, RequestBuilder, Client]](action: Action[Request, Response, RequestBuilder, Client], request: Request, listener: ActionListener[Response]): Unit = ???

  override def threadPool(): ThreadPool = ???

  override def close(): Unit = ???
}
