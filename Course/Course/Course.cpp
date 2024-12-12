#include <iostream>
#include <fstream>

using namespace std;

//Функция для удаления матрицы
void DeleteMatrix(double** matrix, size_t size)
{
    for (size_t i = 0; i < size; ++i)
    {
        delete[] matrix[i];
    }
    delete[] matrix;
}

//Функция, выделяющая память под матрицу
double** MemoryMatrix(size_t size, bool initialize = true, double initialValue = double())
{
    double** matrix = new double* [size];
    for (size_t i = 0; i < size; ++i)
    {
        matrix[i] = new double[size];
        if (initialize)
        {
            for (size_t j = 0; j < size; ++j)
            {
                matrix[i][j] = initialValue;
            }
        }
    }
    return matrix;
}

//Функция для создания матрицы
double** CreateMatrix(size_t size)
{
    double** matrix = MemoryMatrix(size, false);
    cout << "Введите элементы матрицы построчно:" << endl;
    for (size_t i = 0; i < size; ++i)
    {
        for (size_t j = 0; j < size; ++j)
        {
            cin >> matrix[i][j];
        }
    }
    return matrix;
}

//Функция, выводящая матрицу на консоли
void PrintMatrix(double** matrix, size_t size)
{
    for (size_t i = 0; i < size; ++i)
    {
        for (size_t j = 0; j < size; ++j)
        {
            cout << matrix[i][j] << " ";
        }
        cout << endl;
    }
}

//Функция, загружающая матрицу  из файла 
void LoadMatrix(const string& filename, double** matrix, size_t size) 
{
    ifstream file(filename);
    if (file.is_open())
    {
        for (int i = 0; i < size; ++i) 
        {
            for (int j = 0; j < size; ++j) 
            {
                file >> matrix[i][j];
            }
        }
        file.close();
        cout << "Матрица была загружена из файла " << filename << endl;
    }
    else {
        cerr << "Не удалось открыть файл " << filename << endl;
    }
}

//Функция, сохраняющая матрицу в файл
void SaveMatrix(const string&  filename, double** matrix, size_t size) 
{
    ofstream file(filename);
    if (file.is_open()) {
        for (int i = 0; i < size; ++i) 
        {
            for (int j = 0; j < size; ++j) 
            {
                file << matrix[i][j] << " ";
            }
            file << "\n";
        }
        file.close();
        cout << "Матрица была сохранена в файл " << filename << endl;
    }
    else {
        cerr << "Не удалось открыть файл " << filename << endl;
    }
}

//Функция, редактирующая элементы матрицы
void EditMatrix(double** matrix, size_t size) 
{
    size_t rows, cols, element;
    cout << "Введите номер строки и столбца элемента для редакции: ";
    cin >> rows >> cols;
    cout << "Введите новое значение элемента: ";
    cin >> element;
    if (rows >= 0 and cols >= 0)
    {
        matrix[rows][cols] = element;
    }
    else 
    {
        cerr << "Введены неверные координаты элемента." << endl;
    }
}

//Функция, которая выводит номера строк всех матриц в порядке убывания среднего арифметического элементов этих строк (по каждой из матриц отдельно)
void TaskMatrix(double** matrix, size_t size) 
{
    double* averages = new double[size];
    int* indices = new int[size];
    for (size_t i = 0; i < size; ++i) {
        double sum = 0;
        for (size_t j = 0; j < size; ++j)
        {
            sum += matrix[i][j];
        }
        averages[i] = sum / size;
        indices[i] = i + 1;
    }
    for (size_t i = 0; i < size - 1; ++i) 
    {
        for (size_t j = 0; j < size - i - 1; ++j)
        {
            if (averages[j] < averages[j + 1])
            {
                double tempAv = averages[j];
                averages[j] = averages[j + 1];
                averages[j + 1] = tempAv;
                int tempIndex = indices[j];
                indices[j] = indices[j + 1];
                indices[j + 1] = tempIndex;
            }
        }
    }
    cout << "Номера строк в порядке убывания среднего арифметического: ";
    for (size_t k = 0; k < size; k++)
    {
        cout << indices[k] << " ";
    }
    cout << endl;
    delete[] averages;
    delete[] indices;
}

//Меню программы
int main()
{
    setlocale(LC_ALL, "RUS");
    size_t size;
    cout << "Введите размер матриц A, B и C: ";
    cin >> size;
    cout << "Матрица A." << endl;
    double** A = CreateMatrix(size);
    cout << "Матрица B." << endl;
    double** B = CreateMatrix(size);
    cout << "Матрица C." << endl;
    double** C = CreateMatrix(size);
    
    while (true)
    {
        int command, command_matrix = 0;
        cout << "Выберите команду:" << endl << "1 - Консольный ввод, 2 - Консольный вывод, 3 - Файловый ввод, 4 - Файловый вывод, 5 - Редактировать матрицы," << endl << "6 - Вывести номера строк всех матриц в порядке убывания среднего арифметического элементов этих строк," << endl << "Введите любое другой символ для выхода из программы." << endl;
        cin >> command;
        if (command == 1)
        {
            cout << "Выберите матрицу, которую хотите ввести: 1 - A, 2 - B, 3 - C." << endl;
            cin >> command_matrix;
            if (command_matrix == 1)
            {
                DeleteMatrix(A, size);
                double** A = CreateMatrix(size);
            }
            else if (command_matrix == 2)
            {
                DeleteMatrix(B, size);
                double** B = CreateMatrix(size);
            }
            else if (command_matrix == 3)
            {
                DeleteMatrix(C, size);
                double** C = CreateMatrix(size);
            }
        }
        else if (command == 2)
        {
            cout << "Выберите матрицу, которую хотите вывести: 1 - A, 2 - B, 3 - C." << endl;
            cin >> command_matrix;
            if (command_matrix == 1)
            {
                PrintMatrix(A, size);
            }
            else if (command_matrix == 2)
            {
                PrintMatrix(B, size);
            }
            else if (command_matrix == 3)
            {
                PrintMatrix(C, size);
            }
        }
        else if (command == 3)
        {
            cout << "Выберите матрицу, которую хотите ввести через файл: 1 - A, 2 - B, 3 - C." << endl;
            cin >> command_matrix;
            if (command_matrix == 1)
            {
                LoadMatrix("Matrix_A.txt", A, size);
            }
            else if (command_matrix == 2)
            {
                LoadMatrix("Matrix_B.txt", B, size);
            }
            else if (command_matrix == 3)
            {
                LoadMatrix("Matrix_C.txt", C, size);
            }
        }
        else if (command == 4)
        {
            cout << "Выберите матрицу, которую хотите ввести через файл: 1 - A, 2 - B, 3 - C." << endl;
            cin >> command_matrix;
            if (command_matrix == 1)
            {
                SaveMatrix("Matrix_A.txt", A, size);
            }
            else if (command_matrix == 2)
            {
                SaveMatrix("Matrix_B.txt", B, size);
            }
            else if (command_matrix == 3)
            {
                SaveMatrix("Matrix_C.txt", C, size);
            }
        }
        else if (command == 5)
        {
            cout << "Выберите матрицу, которую хотите отредактировать: 1 - A, 2 - B, 3 - C." << endl;
            cin >> command_matrix;
            if (command_matrix == 1)
            {
                EditMatrix(A, size);
            }
            else if (command_matrix == 2)
            {
                EditMatrix(B, size);
            }
            else if (command_matrix == 3)
            {
                EditMatrix(C, size);
            }
        }
        else if (command == 6)
        {
            TaskMatrix(A, size);
            TaskMatrix(B, size);
            TaskMatrix(C, size);
        }
        else
        {
            return 0;
        }
    }
    return 0;
}