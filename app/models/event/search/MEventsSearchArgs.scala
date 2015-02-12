package models.event.search

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.02.15 16:49
 * Description: Поиск по MEvent: интерфейс и реализация.
 */

/** Для поиска по событиям используется сий интерфейс аргументов dyn-поиска. */
trait IEventsSearchArgs extends OwnerId with OnlyUnseen with WithDateSort


/** Дефолтовая реализация [[IEventsSearchArgs]]. */
case class MEventsSearchArgs(
  ownerId       : Option[String] = None,
  onlyUnseen    : Boolean = false,
  withDateSort  : Option[Boolean] = None,
  override val returnVersion: Option[Boolean] = None,
  maxResults    : Int = 10,
  offset        : Int = 0
) extends IEventsSearchArgs
