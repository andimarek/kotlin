// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75874

// "Change type from 'String' to '(Int) -> String'" "true"
fun foo(param: ((Int) -> String) -> String) {
    foo <!ARGUMENT_TYPE_MISMATCH!>{
        f: String -> <!UNRESOLVED_REFERENCE!>f<!>(42)
    }<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral */
