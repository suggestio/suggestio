package io.suggest.model.common

import com.fasterxml.jackson.annotation.JsonIgnore

/** Добавить поле id: Option[String] */
trait OptStrId {
   @JsonIgnore
   def id: Option[String]
 }
