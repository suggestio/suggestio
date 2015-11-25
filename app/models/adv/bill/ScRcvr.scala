package models.adv.bill

import java.sql.Connection

import com.google.inject.Singleton
import io.suggest.ym.model.common.{SinkShowLevels, AdShowLevels}
import models.SinkShowLevel
import util.sqlm.SqlFieldsGenerators
import util.anorm.AnormPgArray._
import anorm._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.11.15 15:47
 * Description: Модель данных размещения в выдаче кокретного узла.
 */
@Singleton
class ScRcvr_ {

  object Fields extends SqlFieldsGenerators {

    val RCVR_ID_FN = "rcvr_id"
    val SLS_FN     = "sls"

    /** Метод, возвращающий список полей, который можно пройти хотя бы один раз. */
    override val FIELDS = Seq(RCVR_ID_FN, SLS_FN)

  }


  object Parsers {

    import anorm.SqlParser._
    import Fields._

    val RCVR_ID = str(RCVR_ID_FN)

    val SLS     = {
      get[Set[String]]("show_levels") map { slsRaw =>
        slsRaw.map { slRaw =>
          val result = if (slRaw.length == 1) {
            // compat: парсим slsPub, попутно конвертя их в sink-версии
            val sl = AdShowLevels.withName(slRaw)
            SinkShowLevels.fromAdSl(sl)
          } else {
            SinkShowLevels.withName(slRaw)
          }
          result : SinkShowLevel
        }
      }
    }

    val ROW = RCVR_ID ~ SLS map {
      case rcvrId ~ sls =>
        ScRcvr(
          rcvrId = rcvrId,
          sls    = sls
        )
    }

  }


  def anormParams(o: ScRcvr)(implicit c: Connection): List[NamedParameter] = {
    import Fields._
    List(
      RCVR_ID_FN  -> o.rcvrId,
      SLS_FN      -> strings2pgArray(SinkShowLevels.sls2strings( o.sls ))
    )
  }

}


case class ScRcvr(
  rcvrId  : String,
  sls     : Set[SinkShowLevel]
)
