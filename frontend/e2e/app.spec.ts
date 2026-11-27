import { expect, test } from '@playwright/test';

test('shows the Merguez welcome page', async ({ page }) => {
  await page.goto('/');
  await expect(
    page.getByRole('heading', {
      level: 1,
      name: 'Vos sessions de grappling, simplement organisées.',
    }),
  ).toBeVisible();
});
