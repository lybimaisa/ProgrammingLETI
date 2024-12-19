#include <iostream>
#include <string>
#include <limits>
#define NOMINMAX
#include <Windows.h>
using namespace std;

struct Student
{
    string name;
    int group;
    int grade[5];
    double CoolGrade() const
    {
        int sum = 0;
        for (int i = 0; i < 5; ++i)
            sum += grade[i];
        return static_cast<double>(sum) / 5;
    }

    bool BadGrade() const
    {
        for (int i = 0; i < 5; ++i)
            if (grade[i] == 2)
                return true;
        return false;
    }
};

struct Summary
{
    int group;
    int StudentSum;
    int BadGradeSum;
};

void Center(const string& str, int width)
{
    int len = str.length();
    int Left = (width - len) / 2;
    int Right = width - len - Left;
    cout << string(Left, ' ') << str << string(Right, ' ');
}

void SortGroup(Student* members, int n) 
{
    for (int k = n / 2; k > 0; k /= 2)
    {
        for (int i = k; i < n; ++i)
        {
            Student temp = members[i];
            int j;
            for (j = i; j >= k and members[j - k].group > temp.group; j -= k)
            {
                members[j] = members[j - k];
            }
            members[j] = temp;
        }
    }
}

void PrintStudents(const Student* members, int n) 
{
    for (int i = 0; i < n; ++i) 
    {
        cout << members[i].group << " - " << members[i].name << ": ";
        string grade;
        for (int j = 0; j < 5; ++j) 
        {
            grade += to_string(members[i].grade[j]);
            if (j < 4) grade += ", ";
        }
        cout << grade << endl;
    }
}

int HighStudents(const Student* members, Student* coolmembers, int n)
{
    int m = 0;
    for (int i = 0; i < n; ++i) 
    {
        if (members[i].CoolGrade() > 4.0)
        {
            coolmembers[m++] = members[i];
        }
    }
    return m;
}

void SortHighStudents(Student* members, int n)
{
    for (int k = n / 2; k > 0; k /= 2)
    {
        for (int i = k; i < n; ++i)
        {
            Student temp = members[i];
            int j;
            for (j = i; j >= k and members[j - k].CoolGrade() < temp.CoolGrade(); j -= k)
            {
                members[j] = members[j - k];
            }
            members[j] = temp;
        }
    }
}

void PrintHighStudents(const Student* members, int n) 
{
    for (int i = 0; i < n; ++i)
    {
        double coolnumber = members[i].CoolGrade();
        cout << members[i].group << ", " << members[i].name << " - " << coolnumber << endl;
    }
}

int SumStudents(const Student* members, Summary* sum, int n)
{
    int count = 0;
    for (int i = 0; i < n;)
    {
        Summary countsummary;
        countsummary.group = members[i].group;
        countsummary.StudentSum = 0;
        countsummary.BadGradeSum = 0;
        while (i < n and members[i].group == countsummary.group)
        {
            countsummary.StudentSum++;
            if (members[i].BadGrade())
            {
                countsummary.BadGradeSum++;
            }
            i++;
        }
        sum[count++] = countsummary;
    }
    return count;
}

void SortSumStudents(Summary* members, int n)
{
    for (int k = n / 2; k > 0; k /= 2)
    {
        for (int i = k; i < n; ++i)
        {
            Summary temp = members[i];
            int j;
            for (j = i; j >= k and members[j - k].BadGradeSum < temp.BadGradeSum; j -= k)
            {
                members[j] = members[j - k];
            }
            members[j] = temp;
        }
    }
}

void PrintSumStudents(const Summary* sum, int count, bool isHuman)
{
    if (isHuman)
    {
        cout << endl << "Подсчёт студентов в группе и студентов, у которых есть хотя бы одна 2.\n" << endl;
        const int GroupWidth = 15;
        const int StudentsWidth = 20;
        const int BadGradeWidth = 25;
        auto truncate = [](const string& str, size_t width) -> string
        {
            if (str.size() > width)
            {
                return str.substr(0, width - 3) + "...";
            }
            return str;
        };
        Center("Группа", GroupWidth);
        cout << "|";
        Center("Количество студентов", StudentsWidth);
        cout << "|";
        Center("Количество студентов с 2", BadGradeWidth);
        cout << endl;
        cout << string(GroupWidth, '-') << "+" << string(StudentsWidth, '-') << "+" << string(BadGradeWidth, '-') << endl;
        for (int i = 0; i < count; ++i)
        {
            Center(truncate(to_string(sum[i].group), GroupWidth), GroupWidth);
            cout << "|";
            Center(truncate(to_string(sum[i].StudentSum), StudentsWidth), StudentsWidth);
            cout << "|";
            Center(truncate(to_string(sum[i].BadGradeSum), BadGradeWidth), BadGradeWidth);
            cout << endl;
        }
    }
    else
    {
        for (int i = 0; i < count; ++i)
        {
            cout << sum[i].group << " - " << sum[i].StudentSum << " - " << sum[i].BadGradeSum << endl;
        }
    }
}

int main(int argc, const char* argv[])
{
    SetConsoleCP(1251);
    SetConsoleOutputCP(1251);
    bool isHuman = false;
    if (argc <= 1 || strcmp(argv[1], "false") != 0)
    {
        isHuman = true;
    }
    int n;
    if (isHuman)
    {
        cout << "Введите количество студентов в потоке: ";
    }
    cin >> n;
    cin.ignore(numeric_limits<streamsize>::max(), '\n');
    Student* members = new Student[n];
    for (int i = 0; i < n; ++i)
    {
        if (isHuman)
        {

            cout << i+1 << " студент.\n" << "Введите ФИО студента: ";
        }
        getline(cin, members[i].name);
        if (isHuman)
        {
            cout << "Введите группу студента: ";
        }
        cin >> members[i].group;
        if (isHuman)
        {
            cout << "Введите 5 оценок студента: ";
        }
        for (int j = 0; j < 5; ++j)
        {
            cin >> members[i].grade[j];
        }
        cin.ignore(numeric_limits<streamsize>::max(), '\n');
    }
    SortGroup(members, n);
    if (isHuman)
    {
        cout << "Полный список студентов по возрастанию номера группы:" << endl;
    }
    PrintStudents(members, n);
    Student* coolmembers = new Student[n];
    int m = HighStudents(members, coolmembers, n);
    SortHighStudents(coolmembers, m);

    if (m > 0) 
    {
        if (isHuman) 
        {
            cout << endl << "Студенты со средним баллом > 4.0, упорядоченные по убыванию среднего балла:" << endl;
        }
        PrintHighStudents(coolmembers, m);
    }
    else 
    {
        if (isHuman)
        {
            cout << endl << "Студенты со средним баллом > 4.0 отсутствуют в потоке." << endl;
        }
        else
        {
            cout << "NO" << endl;
        }
    }
    Summary* sum = new Summary[n];
    int SummaryCount = SumStudents(members, sum, n);
    SortSumStudents(sum, SummaryCount);
    PrintSumStudents(sum, SummaryCount, isHuman);
    delete[] members;
    delete[] coolmembers;
    delete[] sum;
    return 0;
}

