#include <iostream>
using namespace std;


void DeleteMatrix(double** matrix, size_t rows)
{
    for (size_t i = 0; i < rows; ++i)
    {
        delete[] matrix[i];
    }
    delete[] matrix;
}

double** MemoryMatrix(size_t rows, size_t cols, bool initialize = true, double initialValue = double()) 
{
    double** matrix = new double* [rows];
    for (size_t i = 0; i < rows; ++i) 
    {
        matrix[i] = new double[cols];
        if (initialize) 
        {
            for (size_t j = 0; j < cols; ++j) 
            {
                matrix[i][j] = initialValue;
            }
        }
    }
    return matrix;
}

double** CreateIdentityMatrix(size_t size) 
{
    double** matrix = MemoryMatrix(size, size, true, 0);
    for (size_t i = 0; i < size; ++i) 
    {
        matrix[i][i] = 1;
    }
    return matrix;
}

double** CreateMatrix(size_t rows, size_t cols, bool isHuman) 
{
    double** matrix = MemoryMatrix(rows, cols, false);
    if (isHuman) 
    {
        cout << "Введите элементы матрицы построчно:" << endl;
    }
    for (size_t i = 0; i < rows; ++i) 
    {
        for (size_t j = 0; j < cols; ++j) 
        {
            cin >> matrix[i][j];
        }
    }
    return matrix;
}

void PrintMatrix(double** matrix, size_t rows, size_t cols) 
{
    for (size_t i = 0; i < rows; ++i) 
    {
        for (size_t j = 0; j < cols; ++j) 
        {
            cout << matrix[i][j] << (j == cols - 1 ? "" : " ");
        }
        cout << endl;
    }
}

double** MultiplyMatrices(double** A1, double** A2, size_t rows, size_t cols, size_t K)
{
    double** result = MemoryMatrix(rows, K, true, 0);
    for (size_t i = 0; i < rows; ++i)
    {
        for (size_t j = 0; j < K; ++j)
        {
            for (size_t k = 0; k < cols; ++k)
            {
                result[i][j] += A1[i][k] * A2[k][j];
            }
        }
    }
    return result;
}

double** PowerMatrix(double** matrix, int x, size_t N) {
    if (x == 0) 
    {
        return CreateIdentityMatrix(N);
    }
    double** result = CreateIdentityMatrix(N);
    for (int p = 0; p < x; ++p) 
    {
        double** temp = MultiplyMatrices(result, matrix, N, N, N);
        DeleteMatrix(result, N);
        result = temp;
    }

    return result;
}


int main(int argc, const char* argv[])
{
    setlocale(LC_ALL, "RUS");
    bool isHuman = false;
    if (argc <= 1 || strcmp(argv[1], "false") != 0)
    {
        isHuman = true;
    }
    size_t N, M, K;
    if (isHuman)
    {
        cout << "Введите количество строк матрицы A1: ";
    }
    cin >> N;
    if (isHuman)
    {
        cout << "Введите количество столбцов матрицы A1: ";
    }
    cin >> M;
    double** A1 = CreateMatrix(N, M, isHuman);
    int command;
    while (true)
    {
        if (isHuman)
        {
            cout << "Доступные команды: 0 - завершить работу, 1 - вывод матрицы A1, 2 - умножить матрицу A1 на матрицу A2, 3 - возвести матрицу A1 в степень.";
        }
        cin >> command;
        if (command == 0)
        {
            break;
        }
        else if (command == 1)
        {
            PrintMatrix(A1, N, M);
        }
        else if (command == 2)
        {
            if (isHuman)
            {
                cout << "Введите количество столбцов матрицы A2: ";
            }
            cin >> K;
            double** A2 = CreateMatrix(M, K, isHuman);
            double** result = MultiplyMatrices(A1, A2, N, M, K);
            DeleteMatrix(A1, N);
            A1 = result;
            M = K;
            DeleteMatrix(A2, M);
        }
        else if (command == 3) 
        {
            if (N != M) 
            {
                if (isHuman)
                {
                    cout << "Невозможно возвести матрицу в степень.";
                }
                cout << "NO" << endl;
            }
            else 
            {
                int x;
                if (isHuman) 
                {
                    cout << "Введите показатель степени: ";
                }
                cin >> x;
                double** result = PowerMatrix(A1, x, N);
                A1 = result;
            }
        }
    }
    DeleteMatrix(A1, N);
    return 0;
}
