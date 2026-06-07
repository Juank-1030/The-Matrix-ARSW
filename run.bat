@echo off
call mvnw compile -q
java -cp "target\classes" com.the.matrix.arsw.The_matrix_escape.TheMatrixEscapeApplication
