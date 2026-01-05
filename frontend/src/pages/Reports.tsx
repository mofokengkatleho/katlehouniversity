import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Download, FileText, Table as TableIcon, Loader2 } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

export default function Reports() {
  const currentDate = new Date();
  const [month, setMonth] = useState(currentDate.getMonth() + 1);
  const [year, setYear] = useState(currentDate.getFullYear());
  const [loadingPdf, setLoadingPdf] = useState(false);
  const [loadingExcel, setLoadingExcel] = useState(false);
  const { toast } = useToast();

  const months = [
    { value: 1, label: 'January' },
    { value: 2, label: 'February' },
    { value: 3, label: 'March' },
    { value: 4, label: 'April' },
    { value: 5, label: 'May' },
    { value: 6, label: 'June' },
    { value: 7, label: 'July' },
    { value: 8, label: 'August' },
    { value: 9, label: 'September' },
    { value: 10, label: 'October' },
    { value: 11, label: 'November' },
    { value: 12, label: 'December' },
  ];

  const years = Array.from({ length: 5 }, (_, i) => currentDate.getFullYear() - 2 + i);

  const downloadPdf = async () => {
    setLoadingPdf(true);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(
        `http://localhost:8080/api/reports/monthly/export/pdf?month=${month}&year=${year}`,
        {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        }
      );

      if (!response.ok) {
        throw new Error('Failed to download PDF');
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `monthly-report-${year}-${month.toString().padStart(2, '0')}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);

      toast({
        title: 'Success',
        description: 'PDF report downloaded successfully',
      });
    } catch (error) {
      console.error('Failed to download PDF', error);
      toast({
        title: 'Error',
        description: 'Failed to download PDF report',
        variant: 'destructive',
      });
    } finally {
      setLoadingPdf(false);
    }
  };

  const downloadExcel = async () => {
    setLoadingExcel(true);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(
        `http://localhost:8080/api/reports/monthly/export/excel?month=${month}&year=${year}`,
        {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        }
      );

      if (!response.ok) {
        throw new Error('Failed to download Excel');
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `monthly-report-${year}-${month.toString().padStart(2, '0')}.xlsx`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);

      toast({
        title: 'Success',
        description: 'Excel report downloaded successfully',
      });
    } catch (error) {
      console.error('Failed to download Excel', error);
      toast({
        title: 'Error',
        description: 'Failed to download Excel report',
        variant: 'destructive',
      });
    } finally {
      setLoadingExcel(false);
    }
  };

  const selectedMonthName = months.find(m => m.value === month)?.label || '';

  return (
    <div className="container mx-auto p-6 max-w-4xl">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Monthly Reports</h1>
        <p className="text-gray-600 mt-2">
          Generate and download monthly payment reports in PDF or Excel format
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Export Report</CardTitle>
          <CardDescription>
            Select a month and year, then download the report in your preferred format
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Period Selection */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium text-gray-700">Month</label>
              <Select value={month.toString()} onValueChange={(value) => setMonth(Number(value))}>
                <SelectTrigger>
                  <SelectValue placeholder="Select month" />
                </SelectTrigger>
                <SelectContent>
                  {months.map((m) => (
                    <SelectItem key={m.value} value={m.value.toString()}>
                      {m.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-gray-700">Year</label>
              <Select value={year.toString()} onValueChange={(value) => setYear(Number(value))}>
                <SelectTrigger>
                  <SelectValue placeholder="Select year" />
                </SelectTrigger>
                <SelectContent>
                  {years.map((y) => (
                    <SelectItem key={y} value={y.toString()}>
                      {y}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Selected Period Display */}
          <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <p className="text-sm font-medium text-blue-900">
              Selected Period: <span className="font-bold">{selectedMonthName} {year}</span>
            </p>
          </div>

          {/* Download Buttons */}
          <div className="space-y-3">
            <div className="flex flex-col sm:flex-row gap-3">
              <Button
                onClick={downloadPdf}
                disabled={loadingPdf || loadingExcel}
                className="flex-1"
                size="lg"
              >
                {loadingPdf ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Generating PDF...
                  </>
                ) : (
                  <>
                    <FileText className="mr-2 h-4 w-4" />
                    Download PDF
                  </>
                )}
              </Button>

              <Button
                onClick={downloadExcel}
                disabled={loadingPdf || loadingExcel}
                variant="outline"
                className="flex-1"
                size="lg"
              >
                {loadingExcel ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Generating Excel...
                  </>
                ) : (
                  <>
                    <TableIcon className="mr-2 h-4 w-4" />
                    Download Excel
                  </>
                )}
              </Button>
            </div>

            <p className="text-sm text-gray-500 text-center">
              Reports include student payment details, totals collected, and outstanding amounts
            </p>
          </div>

          {/* Report Details */}
          <div className="border-t pt-4">
            <h3 className="font-semibold text-gray-900 mb-3">Report Contents</h3>
            <ul className="space-y-2 text-sm text-gray-600">
              <li className="flex items-start">
                <Download className="mr-2 h-4 w-4 mt-0.5 text-blue-600" />
                <span>Summary statistics (total students, paid count, collection rate)</span>
              </li>
              <li className="flex items-start">
                <Download className="mr-2 h-4 w-4 mt-0.5 text-blue-600" />
                <span>List of students who paid (with payment dates and amounts)</span>
              </li>
              <li className="flex items-start">
                <Download className="mr-2 h-4 w-4 mt-0.5 text-blue-600" />
                <span>List of students with outstanding balances</span>
              </li>
              <li className="flex items-start">
                <Download className="mr-2 h-4 w-4 mt-0.5 text-blue-600" />
                <span>Total revenue collected and total outstanding amounts</span>
              </li>
            </ul>
          </div>
        </CardContent>
      </Card>

      {/* Information Card */}
      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="text-lg">File Formats</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <FileText className="h-5 w-5 text-red-600" />
                <h4 className="font-semibold">PDF Report</h4>
              </div>
              <p className="text-sm text-gray-600">
                Professional formatted report perfect for printing and sharing. Includes formatted
                tables and summary statistics.
              </p>
            </div>

            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <TableIcon className="h-5 w-5 text-green-600" />
                <h4 className="font-semibold">Excel Report</h4>
              </div>
              <p className="text-sm text-gray-600">
                Spreadsheet format with multiple sheets (Summary, Paid Students, Owing Students)
                for data analysis and calculations.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
