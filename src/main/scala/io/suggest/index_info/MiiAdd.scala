package io.suggest.index_info

import MiiFileWithIi._
import IndexInfoStatic._
import org.elasticsearch.client.Client
import scala.concurrent.{ExecutionContext, Future}
import io.suggest.util.SioEsUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.13 17:24
 * Description: Файл с директивами добавления нового индекса.
 */

object MiiAdd extends MiiFileWithIiStaticT[MiiAdd] {

  // Префикс имени файла.
  val prefix = "N" // New

  // Дополнительные ключи, характеризующие состояние инстанса MiiAdd.
  val KEY_INX_ALRDY_EXIST = "exist"
  val KEY_USE_FOR_SEARCH  = "search"

  protected def toResult(dkey: String, iinfo: IndexInfo, m: MiiFileWithIi.JsonMap_t): MiiAdd = {
    val isAlreadyExist = getFromMap(KEY_INX_ALRDY_EXIST, m, false)
    val useForSearch   = getFromMap(KEY_USE_FOR_SEARCH, m, false)
    MiiAdd(iinfo, isAlreadyExist=isAlreadyExist, useForSearch=useForSearch)
  }
}


/**
 * Файл, обозначающий необходимость подключения нового индекса.
 * @param indexInfo Метаданные индекса.
 * @param isAlreadyExist Существует ли уже указанный индекс в elasticsearch? Иначе его надо будет создать.
 * @param useForSearch Если true, то этот индекс должен использоваться для поиска.
 */
case class MiiAdd(indexInfo: IndexInfo, isAlreadyExist:Boolean, useForSearch:Boolean = false) extends MiiFileWithIiT[MiiAdd] {

  import MiiAdd.{KEY_INX_ALRDY_EXIST, KEY_USE_FOR_SEARCH}

  def prefix: String = MiiAdd.prefix


  /**
   * Сконвертить в ActiveMII.
   * @return Экземпляр ActiveMII с текущим indexInfo.
   */
  def toActive = MiiActive(indexInfo)


  /**
   * Экспорт состояния текущего экземпляра.
   * @return Карту, пригодную для сериализации в json.
   */
  override protected def export: JsonMap_t = super.export ++ Map(
    KEY_INX_ALRDY_EXIST -> isAlreadyExist,
    KEY_USE_FOR_SEARCH  -> useForSearch
  )


  /**
   * Убедится, что желаемый индекс существует в ElasticSearch. Готовность индекса -- это наличие индекса и
   * залитый маппинг. Маппинг заливается всегда, а индекс проверяется лишь по наличию.
   */
  def ensureCreated(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    val allShards = indexInfo.allShards
    SioEsUtil.isIndexExist(allShards: _*) flatMap { isExist =>
      if (isAlreadyExist && isExist) {
        futureTrue

      } else if (!isAlreadyExist) {
        // Индекс(ы) ещё не существуют, надо создать бы их.
        ensureIndexCreated

      } else {
        val ex = new RuntimeException("Indexes %s not exist, but they should! Something goes wrong." format allShards)
        Future.failed(ex)
      }
    }
  }


  /**
   * Создать индексы в базе, залить маппинги.
   * @return true, если всё ок.
   */
  def ensureIndexCreated(implicit client:Client, executor:ExecutionContext): Future[Boolean] = {
    indexInfo.ensureShards flatMap {
      case true  => indexInfo.ensureMappings
      case false => futureFalse
    }
  }

}
