#include <iostream>

int main(int argc, char* argv[])
{
	bool isHuman = false;
	if (argc <= 1 || strcmp(argv[1], "false") != 0)
	{
		isHuman = true;
	}
	if (isHuman)
	{
		double x, y;
		setlocale(LC_ALL, "");
		std::wcout << L"Дан треугольник с координатами вершин (0, 0) (3, 0) (0, 4)." << std::endl;
		std::wcout << L"Чтобы проверить, находится ли точка в треугольнике, введите через пробел сначала координату оси x, а затем оси y." << std::endl;
		do
		{
			std::wcout << L"Введите координаты точки:" << std::endl;
			std::cin >> x >> y;
			if (!std::cin)
			{
				std::wcout << L"Введённые данные не являются числом." << std::endl;
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
					std::wcout << L"Точка входит в треугольник." << std::endl;
				}
				else
				{
					std::wcout << L"Точка не входит в треугольник." << std::endl;
				}
			}
			else
			{
				std::wcout << L"Точка не входит в треугольник." << std::endl;
			}
		}
		while (x != 0 or y != 0);
		std::wcout << L"Конец программы." << std::endl;
		system("pause");
		return 0;
	}
	else
	{
		double x, y;
		do
		{
			std::cin >> x >> y;
			if (!std::cin)
			{
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
					std::cout << "YES" << endl;
				}
				else
				{
					std::cout << "NO" << endl;
				}
			}
			else
			{
				std::cout << "NO" << endl;
			}
		}
		while (x != 0 or y != 0);
	}
	return 0;
}