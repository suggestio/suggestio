package models.event.search

import io.suggest.model.search.{ReturnVersion, Offset, Limit}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.02.15 16:49
 * Description: Поиск по MEvent: интерфейс и реализация.
 */

/** Для поиска по событиям используется сий интерфейс аргументов dyn-поиска. */
trait IEventsSearchArgs
  extends OwnerId
  with OnlyUnseen
  with WithDateSort
  with Limit
  with Offset
  with ReturnVersion


/** Дефолтовая реализация [[IEventsSearchArgs]]. */
case class MEventsSearchArgs(
  override val ownerId        : Option[String]    = None,
  override val onlyUnseen     : Boolean           = false,
  override val withDateSort   : Option[Boolean]   = None,
  override val returnVersion  : Option[Boolean]   = None,
  override val limit          : Int               = 10,
  override val offset         : Int               = 0
)
  extends IEventsSearchArgs
