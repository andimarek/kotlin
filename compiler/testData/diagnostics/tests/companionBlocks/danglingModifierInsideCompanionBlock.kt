// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    companion {
        fun t() {
        }
        <!SYNTAX!>for<!> <!SYNTAX!>(<!><!SYNTAX!>a<!> in <!SYNTAX!>1<!><!SYNTAX!>..<!><!SYNTAX!>10<!><!SYNTAX!>)<!> <!FUNCTION_DECLARATION_WITH_NO_NAME!><!SYNTAX!><!>{
            <!UNRESOLVED_REFERENCE!>unresolved<!>
        }<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
