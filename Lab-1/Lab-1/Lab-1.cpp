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
		do
		{
			setlocale(LC_ALL, "");
			std::wcout << L"Введите координаты точки:" << std::endl;
			std::cin >> x >> y;
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
			if (x == 0 and y == 0)
			{
				break;
			}
			if (0 <= x && x <= 3)
			{
				if (0 <= y && y <= (-4 * x / 3 + 4))
				{
					std::cout << "YES";
				}
				else
				{
					std::cout << "NO";
				}
			}
			else
			{
				std::cout << "NO";
			}
		}
		while (x != 0 or y != 0);
	}
	return 0;
}