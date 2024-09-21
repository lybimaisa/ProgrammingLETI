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
		std::wcout << L"Введите координаты точки:" << std::endl;
		std::cin >> x >> y;
		if (0 <= x && x <= 3)
		{
			if (y <= (-4 * x / 3))
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
	else
	{
		double x, y;
		std::cin >> x >> y;
		if (0 <= x && x <= 3)
		{
			if (y <= (-4 * x / 3))
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
	system("pause");
	return 0;
}