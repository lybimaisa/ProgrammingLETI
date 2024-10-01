#include <iostream>

int n, g, i, j, k, k1, k2, k3, x, max, min, a, b, c;
float sum, arf;

int main(int argc, char* argv[])
{
	bool isHuman = false;
	if (argc <= 1 || strcmp(argv[1], "false") != 0)
	{
		isHuman = true;
	}
	k1 = k2 = k3 = sum = max = a = b = c = 0;
	min = 100000;
	if (isHuman)
	{
		setlocale(LC_ALL, "");
		std::wcout << L"Введите целое положительное число - количество чисел последовательности:" << std::endl;
	}
	std::cin >> n;
	for (g = 0; g < n; g++)
	{
		if (isHuman)
		{
			std::wcout << g+1 << L" число:" << std::endl;
		}
		std::cin >> x;
		k = 0;
		for (j = 2; j <= sqrt(x); j++)
		{
			if (x % j == 0)
			{
				k++;
				break;
			}
		}
		if (k == 0 and x != 1)
		{
			if (isHuman)
			{
				std::wcout << x << L" - простое число" << std::endl;
			}
			else
			{
				std::cout << x << std::endl;
			}
		}
		sum += x;
		if (min > x)
		{
			min = x;
		}
		if (max < x)
		{
			max = x;
		}
		if (x % 5 == 0)
		{
			k1 += 1;
		}
		i = x;
		while (i % 2 == 0)
		{
			i = i / 2;
		}
		if (i == 1)
		{
			k2 += 1;
		}
		a = b;
		b = c;
		c = x;
		if ((a + b) < c)
		{
			k3++;
		}
	}
	if (isHuman)
	{
		std::wcout << L"Среднее арифмитическое чисел:" << std::endl;
	}
	arf = round((sum / n) * 100) / 100;
	std::cout << arf << std::endl;
	if (isHuman)
	{
		std::wcout << L"Разность между максимальным и минимальным числами последовательности:" << std::endl;
	}
	std::cout << max - min << std::endl;
	if (isHuman)
	{
		std::wcout << L"Количество чисел кратных 5:" << std::endl;
	}
	std::cout << k1 << std::endl;
	if (isHuman)
	{
		std::wcout << L"Количество чисел, которые являются степенью двойки:" << std::endl;
	}
	std::cout << k2 << std::endl;
	if (isHuman)
	{
		std::wcout << L"Количество чисел, которые превышают сумму 2 предыдущих членов:" << std::endl;
	}
	std::cout << k3 << std::endl;
}

