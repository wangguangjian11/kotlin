// !LANGUAGE: +ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

// See KT-21515 for a class diagram and details

// Object is to prevent accidental short-name import
object O {
    open class Alpha {
        class FromAlpha

        companion object {
            class FromCompanionAlpha
        }
    }

    open class Beta : Alpha() {
        class FromBeta

        companion object {
            class FromCompanionBeta
        }
    }


    open class A {
        class FromA

        companion object : Beta() {
            class FromCompanionA
        }
    }

//////////////////////////

    open class FarAway {
        class FromFarAway

    }

    open class Gamma {
        class FromGamma
        companion object : FarAway() {
            class FromCompanionGamma
        }
    }

    open class B : A() {
        class FromB

        companion object : Gamma() {
            class FromCompanionB
        }
    }
}

///////////////////////////////


open class Delta {
    class FromDelta
}

class C : O.B() {
    companion object : Delta() {
        class FromCompanionC
    }

    // Classifiers from direct superclasses
    val c: FromA? = null
    val d: FromB? = null

    // Classifiers from our own companion
    val n: FromCompanionC? = null

    // NB: we don't see direct superclasses themselves.
    val a: <!UNRESOLVED_REFERENCE!>A<!>? = null
    val b: <!UNRESOLVED_REFERENCE!>B<!>? = null

    // Classifiers from companions of direct superclasses
    val e: <!UNRESOLVED_REFERENCE!>FromCompanionA<!>? = null
    val f: <!UNRESOLVED_REFERENCE!>FromCompanionB<!>? = null

    // NB: as we don't see direct supertypes themselves, we also don't see "cousin" supertypes themselves
    val g: <!UNRESOLVED_REFERENCE!>Alpha<!>? = null
    val h: <!UNRESOLVED_REFERENCE!>Beta<!>? = null
    val i: <!UNRESOLVED_REFERENCE!>Gamma<!>? = null

    // Classifiers from superclasses of companions of direct superclasses <=> classifiers from "cousin" superclasses
    val k: <!UNRESOLVED_REFERENCE!>FromAlpha<!>? = null
    val l: <!UNRESOLVED_REFERENCE!>FromBeta<!>? = null
    val m: <!UNRESOLVED_REFERENCE!>FromGamma<!>? = null

    // Classifiers from companions of superclasses of companions of direct superclasses <=> classifiers from companions of "cousin" superclasses
    val o: <!UNRESOLVED_REFERENCE!>FromCompanionAlpha<!>? = null
    val p: <!UNRESOLVED_REFERENCE!>FromCompanionBeta<!>? = null
    val q: <!UNRESOLVED_REFERENCE!>FromCompanionGamma<!>? = null

    // Classifiers from supertypes of our own companion
    val r: <!UNRESOLVED_REFERENCE!>FromDelta<!>? = null
}