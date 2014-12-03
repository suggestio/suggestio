package models.ai

import scala.beans.BeanProperty

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.14 20:03
 * Description: Описание валюты в рамках ai-моделей.
 */
case class AiCurrency(
  @BeanProperty code: String,
  @BeanProperty course: Float,
  cbrfCodeOpt: Option[Int] = None,
  @BeanProperty count: Int = 1,
  nameOpt: Option[String] = None
)
