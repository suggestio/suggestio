package models.mup

import io.suggest.crypto.hash.HashesHex
import io.suggest.es.model.MEsUuId
import io.suggest.sec.QsbSigner
import io.suggest.xplay.qsb.QueryStringBindableImpl
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.02.2020 18:22
  * Description: signed-модель аргументов для скачивания.
  */
object MDownLoadQs {

  object Fields {
    def NODE_ID = "n"
    def PERSON_ID = "p"
    def VALID_TILL_S = "v"
    def DISP_INLINE = "a"
    def CLIENT_ADDR = "c"
    def SIGNATURE = "s"
    def HASHES = "h"
  }

  implicit def downLoadQsb(implicit
                           esUuIdB: QueryStringBindable[MEsUuId],
                           hashesHexB: QueryStringBindable[HashesHex],
                          ): QueryStringBindable[MDownLoadQs] = {
    new QueryStringBindableImpl[MDownLoadQs] {

      private def strOptB = implicitly[QueryStringBindable[Option[String]]]
      private def longOptB = implicitly[QueryStringBindable[Option[Long]]]
      private def boolB = implicitly[QueryStringBindable[Boolean]]

      private def qsbSigner = new QsbSigner(MUploadTargetQs.SIGN_SECRET, Fields.SIGNATURE)

      override def bind(key: String, params0: Map[String, Seq[String]]): Option[Either[String, MDownLoadQs]] = {
        val F = Fields
        val k = key1F(key)

        for {
          params            <- qsbSigner.signedOrNone(k(""),    params0 )
          nodeIdE           <- esUuIdB.bind( k(F.NODE_ID),      params )
          personIdOptE      <- strOptB.bind( k(F.PERSON_ID),    params )
          validTillSE       <- longOptB.bind( k(F.VALID_TILL_S),   params )
          dispInlineE       <- boolB.bind( k(F.DISP_INLINE),    params )
          clientAddrOptE    <- strOptB.bind( k(F.CLIENT_ADDR),  params )
          hashesHexE        <- hashesHexB.bind( k(F.HASHES),    params )
        } yield {
          for {
            nodeId          <- nodeIdE
            personIdOpt     <- personIdOptE
            validTillS      <- validTillSE
            dispInline      <- dispInlineE
            clientAddrOpt   <- clientAddrOptE
            hashesHex       <- hashesHexE
          } yield {
            MDownLoadQs(
              nodeId        = nodeId,
              personId      = personIdOpt,
              validTillS    = validTillS,
              dispInline    = dispInline,
              clientAddr    = clientAddrOpt,
              hashesHex     = hashesHex,
            )
          }
        }
      }

      override def unbind(key: String, value: MDownLoadQs): String = {
        val F = Fields
        val k = key1F(key)

        val unsigned = _mergeUnbinded1(
          esUuIdB.unbind( k(F.NODE_ID),     value.nodeId ),
          strOptB.unbind( k(F.PERSON_ID),   value.personId ),
          longOptB.unbind( k(F.VALID_TILL_S), value.validTillS ),
          boolB.unbind( k(F.DISP_INLINE),   value.dispInline ),
          strOptB.unbind( k(F.CLIENT_ADDR), value.clientAddr ),
          hashesHexB.unbind( k(F.HASHES),   value.hashesHex ),
        )
        qsbSigner.mkSigned(key, unsigned)
      }

    }
  }

  @inline implicit def univEq: UnivEq[MDownLoadQs] = UnivEq.derive


  def hashesHex = GenLens[MDownLoadQs](_.hashesHex)

}


/** Контейнер реквизитов для закачки.
  *
  * @param nodeId id узла.
  * @param personId сессия текущего юзера, т.к. закачка может быть ограничена по публичности доступа.
  * @param validTillS currentTimeMs/1000 + TTL, секунды.
  *                   None означает, что ссылка без ограничения времени жизни (или это регулируется как-то иначе).
  * @param dispInline Управление заголовком ответа Content-Disposition.
  * @param clientAddr ip-адрес клиента.
  */
case class MDownLoadQs(
                        nodeId        : MEsUuId,
                        personId      : Option[String]      = None,
                        validTillS    : Option[Long]        = None,   // UploadUtil.ttlFromNow()
                        dispInline    : Boolean             = true,
                        clientAddr    : Option[String]      = None,
                        hashesHex     : HashesHex           = Map.empty,
                      )
