package io.suggest.lk.tags.edit.m

import io.suggest.common.tags.search.MTagsFound
import io.suggest.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.12.16 12:24
  * Description: Diode-экшены подсистемы редактора тегов.
  */

sealed trait TagsEditAction extends DAction


case class AddTagFound(tagFace: String) extends TagsEditAction

case class HandleTagsFound(resp: MTagsFound, now: Long) extends TagsEditAction

/** Акшен удаления тега из множества existing-тегов. */
case class RmTag(tagFace: String) extends TagsEditAction

/** Экшен обновления имени тега. */
case class SetTagSearchQuery(query: String) extends TagsEditAction

/** Экшен добавления текущего теста в existing-теги. */
case object AddCurrentTag extends TagsEditAction


case class StartSearchReq(now: Long) extends TagsEditAction
