#include <iostream>

int main(int argc, char* argv[])
{
	bool isHuman = false;
	if (argc <= 1 || strcmp(argv[1], "false") != 0)
	{
		isHuman = true;
	}
	double x, y;
	if (isHuman)
	{
		setlocale(LC_ALL, "");
		std::wcout << L"Дан треугольник с координатами вершин (0, 0) (3, 0) (0, 4)." << std::endl;
		std::wcout << L"Чтобы проверить, находится ли точка в треугольнике, введите через пробел сначала координату оси x, а затем оси y." << std::endl;
		std::wcout << L"Дробные числа следует писать через точку." << std::endl;
	}
	do
	{
		if (isHuman)
		{
			std::wcout << L"Введите координаты точки:" << std::endl;
		}
		std::cin >> x >> y;
		if (!std::cin)
		{
			if (isHuman)
			{
				std::wcout << L"Введённые данные не являются числом или введены некорректно." << std::endl;
			}
			break;
		}
		if (x == 0 and y == 0)
		{
			break;
		}
		if (0 <= x && x <= 3)
		{
			if (0 <= y && y <= (-4 * x / 3 + 4))
			{
				if (isHuman)
				{
					std::wcout << L"Точка входит в треугольник." << std::endl;
				}
				else
				{
					std::cout << "YES" << std::endl;
				}
			}
			else
			{
				if (isHuman)
				{
					std::wcout << L"Точка не входит в треугольник." << std::endl;
				}
				else
				{
					std::cout << "NO" << std::endl;
				}
			}
		}
		else
		{
			if (isHuman)
			{
				std::wcout << L"Точка не входит в треугольник." << std::endl;
			}
			else
			{
				std::cout << "NO" << std::endl;
			}
		}
	} while (x != 0 or y != 0);
	if (isHuman)
	{
		std::wcout << L"Конец программы." << std::endl;
		system("pause");
		return 0;
	}
	else
	{
		return 0;
	}
}