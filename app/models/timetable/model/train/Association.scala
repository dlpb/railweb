package models.timetable.model.train

import java.util.Date

case class Association(transactionType: TransactionType,
                       mainTrainUid: String,
                       associatedTrainUid: String,
                       startDate: Date,
                       endDate: Date,
                       validMonday: Boolean,
                       validTuesday: Boolean,
                       validWednesday: Boolean,
                       validThursday: Boolean,
                       validFriday: Boolean,
                       validSaturday: Boolean,
                       validSunday: Boolean,
                       category: Category,
                       dateIndicator: DateIndicator,
                       location: String,
                       baseSuffixLocation: String,
                       locationSuffix: String,
                       associationType: AssociationType,
                       stpIndicator: StpIndicator
                      )


sealed trait TransactionType extends FormattedToString

case object NewTransaction extends TransactionType

case object DeleteTransaction extends TransactionType

case object ReviseTransaction extends TransactionType



sealed trait Category extends FormattedToString

case object Join extends Category

case object Divide extends Category

case object Next extends Category

case object NoCategory extends Category



sealed trait DateIndicator extends FormattedToString

case object StandardDaytimeOnly extends DateIndicator

case object OverNextMidnight extends DateIndicator

case object OverPreviousMidnight extends DateIndicator

case object NoDateIndicator extends DateIndicator


sealed trait AssociationType extends FormattedToString

case object Passenger extends AssociationType

case object Operating extends AssociationType

case object NoAssociationType extends AssociationType


sealed trait StpIndicator extends FormattedToString

case object Cancellation extends StpIndicator

case object New extends StpIndicator

case object Overlay extends StpIndicator

case object Permanent extends StpIndicator


