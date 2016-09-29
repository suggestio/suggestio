package models.msc

import io.suggest.model.es.MEsId
import io.suggest.model.play.qsb.QueryStringBindableImpl
import models.mgeo.MLocEnv
import play.api.mvc.QueryStringBindable
import io.suggest.ad.search.AdSearchConstants._
import io.suggest.common.empty.EmptyProduct

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.16 15:34
  * Description: Аргументы поиска карточек, компилируемые в MNodeSearch.
  */
object MScAdsSearchQs {

  /** Поддержка интеграции с URL query string через play router. */
  implicit def qsb(implicit
                   esIdOptB   : QueryStringBindable[Option[MEsId]],
                   longOptB   : QueryStringBindable[Option[Long]],
                   intOptB    : QueryStringBindable[Option[Int]],
                   locEnvB    : QueryStringBindable[MLocEnv]
                  ): QueryStringBindable[MScAdsSearchQs] = {
    new QueryStringBindableImpl[MScAdsSearchQs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScAdsSearchQs]] = {
        val k = key1F(key)
        for {
          prodIdOptE    <- esIdOptB.bind  (k(PRODUCER_ID_FN),     params)
          rcvrIdOptE    <- esIdOptB.bind  (k(RECEIVER_ID_FN),     params)
          locEnvE       <- locEnvB.bind   (k(LOC_ENV_FN),         params)
          genOptE       <- longOptB.bind  (k(GENERATION_FN),      params)
          limitOptE     <- intOptB.bind   (k(LIMIT_FN),   params)
          offsetOptE    <- intOptB.bind   (k(OFFSET_FN),  params)
        } yield {
          for {
            prodIdOpt   <- prodIdOptE.right
            rcvrIdOpt   <- rcvrIdOptE.right
            locEnv      <- locEnvE.right
            genOpt      <- genOptE.right
            limitOpt    <- limitOptE.right
            offsetOpt   <- offsetOptE.right
          } yield {
            MScAdsSearchQs(
              prodIdOpt = prodIdOpt,
              rcvrIdOpt = rcvrIdOpt,
              locEnv    = locEnv,
              genOpt    = genOpt,
              limitOpt  = limitOpt,
              offsetOpt = offsetOpt
            )
          }
        }
      }

      override def unbind(key: String, value: MScAdsSearchQs): String = {
        _mergeUnbinded {
          val k = key1F(key)
          Seq(
            esIdOptB.unbind   (k(PRODUCER_ID_FN),     value.prodIdOpt),
            esIdOptB.unbind   (k(RECEIVER_ID_FN),     value.rcvrIdOpt),
            locEnvB.unbind    (k(LOC_ENV_FN),         value.locEnv),
            longOptB.unbind   (k(GENERATION_FN),      value.genOpt),
            intOptB.unbind    (k(LIMIT_FN),   value.limitOpt),
            intOptB.unbind    (k(OFFSET_FN),  value.offsetOpt)
          )
        }
      }
    }
  }

}


/** Интерфейс инстансов модели. */
trait IScAdSearchQs {

  /** Опциональный id продьюсера. */
  def prodIdOpt     : Option[MEsId]

  /** Опциональный id ресивера. */
  def rcvrIdOpt     : Option[MEsId]

  /** Возможные данные по геолокации, маячкам и прочему окружению. */
  def locEnv        : MLocEnv

  /** seed для псевдо-рандомной сортировки. */
  def genOpt        : Option[Long]

  /** Лимит выдачи карточек. */
  def limitOpt      : Option[Int]

  /** Сдвиг выдачи карточек. */
  def offsetOpt     : Option[Int]


  /** Есть ли какие-то полезные данные для поиска карточек?
    * Если false, значит поисковый запрос на базе данных из этого инстанса вернёт вообще все карточки. */
  def hasAnySearchCriterias: Boolean = {
    rcvrIdOpt.nonEmpty ||
      locEnv.nonEmpty ||
      prodIdOpt.nonEmpty
  }

}


/** Контейнер qs-аргументов для поиска карточек. */
case class MScAdsSearchQs(
  override val prodIdOpt     : Option[MEsId]       = None,
  override val rcvrIdOpt     : Option[MEsId]       = None,
  override val locEnv        : MLocEnv             = MLocEnv.empty,
  override val genOpt        : Option[Long]        = None,
  override val limitOpt      : Option[Int]         = None,
  override val offsetOpt     : Option[Int]         = None
)
  extends IScAdSearchQs
  with EmptyProduct
